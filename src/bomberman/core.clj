(ns bomberman.core
  (:gen-class)
  (:require [org.httpkit.server :refer [run-server]]
            [org.httpkit.client :as http]
            [cheshire.core :refer :all]
            [compojure.core :refer :all]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [dotenv :refer [env]]
            [ring.middleware.params :refer [wrap-params]]
            [clojure.core.async :as async :refer [>! chan go]])
  (:import
    [java.net URI]
    [javax.net.ssl
     SNIHostName SNIServerName SSLEngine SSLParameters])
  (:use clj-fuzzy.metrics)
  (:use ring.util.codec)
  (:use clojure.walk))

(defn sni-configure
  [^SSLEngine ssl-engine ^URI uri]
  (let [^SSLParameters ssl-params (.getSSLParameters ssl-engine)]
    (.setServerNames ssl-params [(SNIHostName. (.getHost uri))])
    (.setSSLParameters ssl-engine ssl-params)))

(def sni-client (org.httpkit.client/make-client
                  {:ssl-configurer bomberman.core/sni-configure}))

(def slack-url (env "SLACK_URL"))
(def dogs-url "https://dog.ceo/api/breed/race/images/random")

(defn parse-body [req]
  (let [split-text (str/split (get req "text") #" ")]
    {:race (first split-text)
     :quantity (Integer/parseInt (second split-text))
     :user-name (get req "user_name")}))

(defn post-slack-message [message]
  @(http/request {:url slack-url
                  :method :post
                  :body (json/write-str {:text message :parse "full"})}))

(defn build-welcome-message [race user-name quantity]
  (str race " bomb for @" user-name " x" quantity))

(defn create-dogs-url [race]
  (str/replace dogs-url #"race" race))

(defn fetch-dog-image-url [race]
  (->
    race
    create-dogs-url
    (http/get {:client sni-client})
    deref
    :body
    json/read-str
    (get "message")))

(defn request-handler [req]
  (let [{race :race
         quantity :quantity
         user-name :user-name} (parse-body (:form-params req))]
    (if (> quantity 5)
      {:status 200 :body (str "Quantity has the limit of 5, you're passing: " quantity)}
      (do (post-slack-message (build-welcome-message race user-name quantity))
          (doseq [_ (range quantity)]
            (go (>! (chan) (post-slack-message (fetch-dog-image-url race)))))
          {:status 200}))))

(defroutes app
  (POST "/" req (request-handler req)))

(defn -main [& args]
  (run-server (-> app wrap-params) {:port 8080})
  (println "Server started on port 8080"))
