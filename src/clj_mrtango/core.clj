(ns clj-mrtango.core
  (:require [ring.util.codec :as ring-codec]
            [digest]
            [pandect.algo.sha256 :refer :all]
            [clj-http.client :as http]
            [clojure.data.json :as json])
  (:import java.util.Base64
           (javax.crypto Mac)
           (javax.crypto.spec SecretKeySpec))
  (:refer-clojure :exclude [send])
  (:gen-class))


(defn create-nonce-generator
  [& [start-from]]
  (let [start-from (or start-from (System/currentTimeMillis))
        nonce-holder (atom start-from)]
    (fn []
      (locking nonce-holder 
        (reset! nonce-holder (inc @nonce-holder))))))

(def nonce-generator (create-nonce-generator))

(defn encode [to-encode]
  (if (string? to-encode)
    (.encodeToString (Base64/getEncoder) (.getBytes to-encode))
    (String. (.encode (Base64/getEncoder) to-encode)
             "ISO-8859-1")))

(defn secretKeyInst [key mac]
    (SecretKeySpec. (.getBytes key "ISO-8859-1") (.getAlgorithm mac)))

(defn toString [bytes]
    "Convert bytes to a String"
    (String. bytes "ISO-8859-1"))

(defn toHexString [bytes]
  "Convert bytes to a String"
  (apply str (map #(format "%02x" %) bytes)))


(defn stringify
  [data]
  (ring-codec/form-encode data))

(defn sign
  [nonce data command-url key]
  (let [hashstring (str nonce data)
        hashed (String. (sha256-bytes hashstring)
                        "ISO-8859-1")
        to-sign (str command-url hashed)
        mac (Mac/getInstance "HmacSHA512")
        secret-key (secretKeyInst key mac)
        x (-> (doto mac
                (.init secret-key)
                (.update (.getBytes to-sign "ISO-8859-1")))
              .doFinal)]
    (encode x)))

(defn make-private-call
  [conf command data]
  (let [command (or command "/transaction/getBalance")
        nonce (nonce-generator)
        data (assoc data :username (:user (:credentials conf)))
        data (assoc data :nonce nonce)
        data (stringify data)
        url (str (:url conf) "/"
                 (:base conf)
                 command)
        command-url (str
                     "/"
                     (:base conf)
                     command)
        r {:timeout 1000
           
           :headers {"X-API-KEY" (:key (:credentials conf))
                     "X-API-NONCE" nonce
                     "X-API-SIGN" (sign nonce data
                                        command-url
                                        (:secret (:credentials conf)))
                     "Content-Type" "application/x-www-form-urlencoded"
                     } 
           :method "POST"
           :url url
           :body data
           :insecure? true}]
    (json/read-str
     (:body (http/request r))
     :key-fn keyword)))

(defn make-public-call
  [conf command data]
  (let [command (or command "/utility/checkIban")
        nonce (nonce-generator)
        data (assoc data :username (:user (:credentials conf)))
        data (assoc data :nonce nonce)
        data (stringify data)
        url (str (:url conf) "/"
                 (:base conf)
                 command)
        command-url (str
                     "/"
                     (:base conf)
                     command)
        r {:timeout 1000
           
           :headers {"X-API-KEY" (:key (:credentials conf))
                     "X-API-NONCE" nonce
                     "X-API-SIGN" (sign nonce data
                                        command-url
                                        (:secret (:credentials conf)))
                     "Content-Type" "application/x-www-form-urlencoded"
                     } 
           :method "GET"
           :url url
           :body data
           :insecure? true}]
    (json/read-str
     (:body (http/request r))
     :key-fn keyword)))


(defn check-iban
  [conf data]
  (let [data (if (string? data)
               {:iban data}
               data)
        ]
    (make-public-call
     conf "/utility/checkIban" data)))

(defmacro
  defcall
  [n u]
  `(def ~n
     (fn [conf# data#]
       (make-private-call
        conf# ~u data#))))

(defcall get-user "/user")

(defcall get-balance "/transaction/getBalance")

(defcall get-session-data "/user/getSessionData")

(defcall get-transactions-list
  "/transaction/getList")

(defcall get-transactions-list-3
  "/transaction/getList3")

(defcall send "/transaction/sendMoney")
