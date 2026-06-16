(ns {{name}}.tg-auth
   (:require
    [clojure.data.codec.base64 :as base64]
    [clojure.data.json :as json]
    [clojure.string :as string])
   (:import [java.util.zip GZIPInputStream]
            [java.io ByteArrayInputStream ByteArrayOutputStream]
            (javax.crypto Mac)
            (javax.crypto.spec SecretKeySpec))
  )
  
  (import '[javax.crypto Mac]
          '[javax.crypto.spec SecretKeySpec]
          '[java.security MessageDigest]
          '[org.apache.commons.codec.binary Hex])
  
  
  (defn hmac-sha256
    "Вычисляет HMAC-SHA256 подпись"
    [secret-key message]
    (let [mac (Mac/getInstance "HmacSHA256")
          key-spec (SecretKeySpec. secret-key "HmacSHA256")]
      (.init mac key-spec)
      (.doFinal mac message)))
  
  (defn parse-query-params [query]
    (->> (string/split query #"&")
         (map #(let [[k v] (string/split % #"=")]
                 [k (java.net.URLDecoder/decode v "UTF-8")]))
         (into {})))
  
  (defn create-data-check-string [init-data]
    (->> init-data
         (remove #(= (key %) "hash"))
         (sort-by key)
         (map (fn [[k v]] (str k "=" v)))
         (string/join "\n")))
  
  
  
(defn validate-telegram-init-data [bot-token init-data-str] 
  (let [init-data (parse-query-params init-data-str)
        secret-key (hmac-sha256 (.getBytes "WebAppData") (.getBytes bot-token))
        data-check-string (create-data-check-string init-data)
        computed-hash (hmac-sha256 secret-key (.getBytes data-check-string))
        received-hash (get init-data "hash")] 
  
    (when (and received-hash computed-hash)
      (let [received-hex (subs received-hash 0 32)
            computed-hex (->> computed-hash
                              (take 16)
                              (map #(format "%02x" %))
                              (apply str))]
        (= computed-hex received-hex)))))
  
  
  
  (defn parse-query-params-hash [query]
    (when query
      (try
        (let [decoded (java.net.URLDecoder/decode query "UTF-8")
              lines (string/split decoded #"\n")]
          (reduce
           (fn [acc line]
             (if-let [[k v] (->> (string/split line #"=" 2)
                                 (seq)
                                 (map string/trim))]
               (assoc acc k v)
               acc))
           {}
           lines))
        (catch Exception e
          (println "Error parsing Telegram init data:" (.getMessage e))
          {}))))
  
  
  
  (defn sha256-bytes [^String s]
    (.digest (MessageDigest/getInstance "SHA-256") (.getBytes s "UTF-8")))
  
  (defn hmac-sha256-bytes [^bytes key ^String data]
    (let [mac (Mac/getInstance "HmacSHA256")
          secret-key (SecretKeySpec. key "HmacSHA256")]
      (.init mac secret-key)
      (.doFinal mac (.getBytes data "UTF-8"))))
  
  (defn decode-url-utf8 [s]
    (java.net.URLDecoder/decode s "UTF-8"))
  
(defn verify-telegram-hash
  [bot-token data]
  ;; 1. URL decode
  (let [decoded (decode-url-utf8 data)
          ;; 2. Split by \n, then by =, only at first =
          pairs (map #(string/split % #"=" 2)
                     (string/split decoded #"\n"))
          ;; 3. Make a map
          params (into {} pairs)
          provided-hash (get params "hash")
          params-no-hash (dissoc params "hash")
          ;; 4. Alphabetically sort keys, build data-check-string
          data-check-string (->> params-no-hash
                                 (sort-by key)
                                 (map (fn [[k v]] (str k "=" v)))
                                 (string/join "\n"))
          ;; 5. Compute secret key (SHA256 of bot_token)
          secret-key (sha256-bytes bot-token)
          ;; 6. HMAC-SHA256
        computed-hmac-bytes (hmac-sha256-bytes secret-key data-check-string)
        computed-hash (Hex/encodeHexString computed-hmac-bytes)] 
    (= computed-hash provided-hash)))
  
  
  
(defn wrap-telegram-auth [handler bot-token] 
  (fn [request]
    (let [user-data-hash (or (get-in request [:headers "x-telegram-hash"])
                             (get-in request [:headers "X-Telegram-Hash"]))
          init-data (or (get-in request [:headers "x-telegram-initdata"])
                        (get-in request [:headers "X-Telegram-InitData"]))] 
  
      (cond
        (and init-data (not (string/blank? init-data)) (not (= "null" init-data)))
          (if (validate-telegram-init-data bot-token init-data)
            (handler request)
            {:status 401 :body "Invalid Telegram authentication"})
  
  
          (and user-data-hash (not (string/blank? user-data-hash)) (not (= "null" user-data-hash)))
          (if (verify-telegram-hash bot-token user-data-hash)
            (handler request)
            {:status 401 :body "Invalid Telegram authentication"})
  
          :else
          {:status 401 :body "Telegram authentication data missing"}))))
