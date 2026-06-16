(ns {{name}}.tg-auth
  (:require
   [clojure.data.codec.base64 :as base64]
   [clojure.data.json :as json]
   [clojure.string :as s])
  (:import [java.util.zip GZIPInputStream]
           [java.io ByteArrayInputStream ByteArrayOutputStream]
           (javax.crypto Mac)
           (javax.crypto.spec SecretKeySpec)))


(defn hmac-sha256
  "Вычисляет HMAC-SHA256 подпись"
  [secret-key message]
  (let [mac (Mac/getInstance "HmacSHA256")
        key-spec (SecretKeySpec. secret-key "HmacSHA256")]
    (.init mac key-spec)
    (.doFinal mac message)))

(defn parse-query-params [query]
  (->> (s/split query #"&")
       (map #(let [[k v] (s/split % #"=")]
               [k (java.net.URLDecoder/decode v "UTF-8")]))
       (into {})))

(defn create-data-check-string [init-data]
  (->> init-data
       (remove #(= (key %) "hash"))
       (sort-by key)
       (map (fn [[k v]] (str k "=" v)))
       (s/join "\n")))


(defn get-user-id [init-data]
  (try
    (let [user-json (get init-data "user")
          user-map (json/read-str user-json :key-fn keyword)]
      (:id user-map))
    (catch Exception e
      (println "Error parsing user data:" (.getMessage e))
      nil)))

(defn validate-telegram-init-data [bot-token init-data-str]
  (let [init-data (parse-query-params init-data-str)
        telegram_user_id (get-user-id init-data)
        secret-key (hmac-sha256 (.getBytes "WebAppData") (.getBytes bot-token))
        data-check-string (create-data-check-string init-data)
        computed-hash (hmac-sha256 secret-key (.getBytes data-check-string))
        received-hash (get init-data "hash")]

    (when (and received-hash computed-hash)
      (let [received-hex (subs received-hash 0 32) ; Берем первые 32 hex-символа (16 байт)
            computed-hex (->> computed-hash
                              (take 16) ; Берем первые 16 байт
                              (map #(format "%02x" %))
                              (apply str))]


        (when (= computed-hex received-hex)
          telegram_user_id))
      )
    )
  )

(defn wrap-telegram-auth [handler bot-token]
  (fn [request]
    (let [init-data (or (get-in request [:headers "x-telegram-initdata"])
                        (get-in request [:headers "X-Telegram-InitData"]))
          telegram_user_id (get-in request [:params :telegram_user_id])] 
      (if init-data
        (if (= (str telegram_user_id) (str (validate-telegram-init-data bot-token init-data)))
          (handler request)
          {:status 401 :body "Invalid Telegram authentication"})
        {:status 401 :body "Telegram initData missing"}))))