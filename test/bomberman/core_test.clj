(ns bomberman.core-test
  (:require [clojure.test :refer :all]
            [bomberman.core :refer :all]))

(deftest test-parse-body
  (testing "parse-body"
    (is (=
         (parse-body {"text" "bulldog 5"
                      "user_name" "otaviopace"})
         {:race "bulldog"
          :quantity 5
          :user-name "otaviopace"}))))

(deftest test-build-welcome-message
  (testing "build-welcome-message"
    (is (=
         (build-welcome-message "bulldog" "otaviopace" "5")
         "bulldog bomb for @otaviopace x5"))))

(deftest test-create-dogs-url
  (testing "create-dogs-url"
    (is (=
         (create-dogs-url "bulldog")
         "https://dog.ceo/api/breed/bulldog/images/random"))))

(deftest test-fetch-dog-image-url
  (testing "fetch-dog-image-url"
    (is (=
         (subs (fetch-dog-image-url "bulldog") 0 37)
         "https://images.dog.ceo/breeds/bulldog"))))
