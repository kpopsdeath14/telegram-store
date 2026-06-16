(ns {{name}}.core
  (:require
   [org.httpkit.server :as server]
   [ring.middleware.cors :refer [wrap-cors]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.file :refer [wrap-file]]
   [ring.middleware.multipart-params :refer [wrap-multipart-params]]
   [ring.middleware.json :refer [wrap-json-params]]
   [compojure.core :refer :all]
   [clojure.data.json :as json]
   [clj-http.client :as http]
   [ring.middleware.defaults :refer :all]
   [compojure.route :as route] 
   [clojure.string :as s]
   [cheshire.core :as che]
   [clojure.data.codec.base64 :as base64] 
   [clojure.java.io :as io]
   [clojure.edn :as edn]
   [clojure.pprint :as pprint]
   
   [{{name}}.datamodule :as dm]
   [{{name}}.app-data :as ad]
   [{{name}}.tg-auth :as auth]
   [{{name}}.services.cdek :as cdek]
   )
  (:import [java.util.zip GZIPInputStream]
           [java.io ByteArrayInputStream ByteArrayOutputStream]
           (javax.crypto Mac)
           (javax.crypto.spec SecretKeySpec))
  (:gen-class)
  )

(use '[clojure.java.shell :only [sh]])

(def api_keys (ad/app_data :api_keys))

(defonce bot-username-cache (atom {}))


(defn send-telegram-message-with-token
  ([bot-token chat-id text]
   (send-telegram-message-with-token bot-token chat-id text "Markdown" {}))

  ([bot-token chat-id text parse-mode]
   (send-telegram-message-with-token bot-token chat-id text parse-mode {}))

  ([bot-token chat-id text parse-mode reply-markup]
   (let [url (str "https://api.telegram.org/bot" bot-token "/sendMessage")
         base-params {:chat_id (str chat-id)
                      :text text
                      :reply_markup (che/generate-string reply-markup)}
         params (if (and parse-mode (not (s/blank? parse-mode)))
                  (assoc base-params :parse_mode parse-mode)
                  base-params)]

     (try
       (let [response (http/post url
                                 {:form-params params
                                  :content-type :json
                                  :socket-timeout 5000
                                  :conn-timeout 5000
                                  :as :json})
             body (:body response)]

         (if (:ok body)
           (do
             (println "✅ Сообщение отправлено в chat-id:" chat-id)
             {:success true :result body})
           (do
             (println "❌ Ошибка Telegram API для" chat-id ":" (:description body))
             {:success false :error (:description body)})))

       (catch Exception e
         (println "❌ Сетевая ошибка для" chat-id ":" (.getMessage e))
         {:success false :error (.getMessage e)})))))


(defn send-telegram-message
  ([chat-id text]
   (send-telegram-message chat-id text "Markdown" {}))

  ([chat-id text parse-mode]
   (send-telegram-message chat-id text parse-mode {}))

  ([chat-id text parse-mode reply-markup]
   (send-telegram-message-with-token (:bot-token api_keys) chat-id text parse-mode reply-markup)))

(defn set-telegram-bot-description [bot-token description]
  (let [url (str "https://api.telegram.org/bot" bot-token "/setMyDescription")
        params {:description (or description "")}]
    (println "set-telegram-bot-description")
    (println "description length:" (count (or description "")))
    (println "has bot-token:" (boolean bot-token))
    (try
      (let [response (http/post url
                                {:content-type :json
                                 :body (json/write-str params)
                                 :socket-timeout 5000
                                 :conn-timeout 5000
                                 :as :json
                                 :throw-exceptions false})
            status (:status response)
            body (:body response)]
        (println "telegram response status:" status)
        (println "telegram response body:" body)
        (if (:ok body)
          {:ok true}
          {:ok false :error (:description body)}))
      (catch Exception e
        (println "Error setting bot description:" (.getMessage e))
        {:ok false :error (.getMessage e)})
      )
    )
  )

(defn fetch-telegram-bot-description [bot-token]
  (when bot-token
    (try
      (let [url (str "https://api.telegram.org/bot" bot-token "/getMyDescription")
            response (http/get url
                               {:socket-timeout 5000
                                :conn-timeout 5000
                                :as :json
                                :throw-exceptions false})
            body (:body response)
            description (get-in body [:result :description])]
        (when (:ok body) description))
      (catch Exception e
        (println "Error fetching bot description:" (.getMessage e))
        nil))))


(defn base64url-encode [s]
  (-> s
      (.getBytes "UTF-8")
      base64/encode
      (String. "UTF-8")
      (s/replace "+" "-")
      (s/replace "/" "_")))

(defn fetch-bot-username [bot-token]
  (when bot-token
    (if-let [cached (get @bot-username-cache bot-token)]
      cached
      (try
        (let [url (str "https://api.telegram.org/bot" bot-token "/getMe")
              response (http/get url
                                 {:socket-timeout 5000
                                  :conn-timeout 5000
                                  :as :json})
              body (:body response)
              username (:username (:result body))]
          (when username
            (swap! bot-username-cache assoc bot-token username))
          username)
        (catch Exception e
          (println "❌ Ошибка получения bot username:" (.getMessage e))
          nil)))))

(defn order-startapp-link [bot-name order-id]
  (let [payload (che/generate-string {:type "order" :order_id (str order-id)})
        encoded (base64url-encode payload)]
    (str "https://t.me/" bot-name "?startapp=" encoded)))


(defn fetch-telegram-user-info [user-id]
  (try
    (let [bot-token (:bot-token api_keys)
          url (str "https://api.telegram.org/bot" bot-token "/getChat")
          response (http/post url
                              {:form-params {:chat_id user-id}
                               :content-type :json
                               :socket-timeout 5000
                               :conn-timeout 5000
                               :as :json})
          body (:body response)
          result (:result body)
          photo (when result (:photo result))
          big-file-id (when photo (:big_file_id photo))
          file-url (when big-file-id
                     (try
                       (let [file-res (http/get (str "https://api.telegram.org/bot" bot-token "/getFile")
                                                {:query-params {:file_id big-file-id}
                                                 :socket-timeout 5000
                                                 :conn-timeout 5000
                                                 :as :json})
                             file-path (get-in file-res [:body :result :file_path])]
                         (when file-path
                           (str "https://api.telegram.org/file/bot" bot-token "/" file-path)))
                       (catch Exception e
                         (println "Error fetching Telegram file for" user-id ":" (.getMessage e))
                         nil)))]

      (if result
        (cond-> result
          file-url (assoc-in [:photo :big_file_url] file-url))
        {:id user-id
         :error (str "No result in response: " body)}))

    (catch Exception e
      (println "Error fetching Telegram user info for" user-id ":" (.getMessage e))
      {:id user-id
       :error (.getMessage e)})))



(defn admin_product_get_unit [req]
  (let [req_body (:params req)
        filters (:filters req_body)
        search_string (:search_string req_body)
        db_res (dm/db_query_sender "" dm/admin_product_get_unit_sql {:filters filters :search_string search_string})]
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}
    )
  )


(defn admin_product_attribute_get_filter [req]
  (let [req_body (:params req)
        filters (:filters req_body)
        db_res (dm/db_query_sender "" dm/admin_product_attribute_get_filter_sql {:attribute_names filters})]
    (println "filters_get")
    (println db_res)
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)})
  )


(defn admin_product_to_archive [req]
  (let [req_body (:params req)
        actual (:actual req_body)
        unit_ids (:unit_ids req_body)
        product_ids (:product_ids req_body)
        db_res (dm/db_query_sender "" dm/admin_product_to_archive_sql {:actual actual :unit_ids unit_ids :product_ids product_ids})]
    (println "admin_product_to_archive")
    (println db_res)
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}))




(defn product_get_one_per_row [req]
  (let [req_body (:params req)
        filters (:filters req_body)
        search_string (:search_string req_body)
        db_res (dm/db_query_sender "" dm/product_get_one_per_row_sql {:filters filters :search_string search_string})]
    (println "product_get_one_per_row")
    (println db_res)
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}
    )
  )


(defn second_lvl_get_config [req]
  (let [req_body (:params req)
        db_res (dm/db_query_sender "" dm/config_sysconfig_admin_2lvl_get_sql {})
        ]
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}
    )
  )


(defn- copy-file-async [old-file new-file]
  (future
    (try
      (io/copy (io/file old-file) (io/file new-file))
      (println "Copied:" old-file "->" new-file)
      {:success true :new-file new-file}
      (catch Exception e
        (println "Error copying" old-file ":" (.getMessage e))
        {:success false :error (.getMessage e)}))))


(defn- process-single-image [product_id old-path images-path]
  (let [db-res (dm/db_query_sender "" dm/product_get_image_name_sql
                                   {:product_id product_id :filename old-path})
        new-name (some-> db-res first :_result :image_name)]
    (when new-name
      (let [old-file (str images-path "/" old-path)
            new-file (str images-path "/" new-name)]
        {:old-path old-path
         :new-name new-name
         :future (copy-file-async old-file new-file)}))))

(defn- wait-for-copies [copy-futures timeout-ms]
  (let [start-time (System/currentTimeMillis)]
    (loop [futures copy-futures]
      (when (and (seq futures)
                 (< (- (System/currentTimeMillis) start-time) timeout-ms))
        (let [done-keys (->> futures
                             (filter (fn [[_ v]] (realized? (:future v))))
                             (map first))]
          (when (seq done-keys)
            (Thread/sleep 100) ; небольшая пауза
            (recur (apply dissoc futures done-keys))))))))

(defn product_filter_attribute_add [req]
  (let [req_body (:params req)
        filters (:filters req_body)
        product_id (first (:attribute_values (first (:filters (first filters)))))
        processed-attrs (if (some #(= "images" (:set_attribute_name %)) filters)
                          (vec (remove #(or (= "images" (:set_attribute_name %))
                                            (= "quantity" (:set_attribute_name %)))
                                       filters))
                          filters)

        db_res (dm/db_query_sender "" dm/product_attribute_add_using_filter_sql processed-attrs) 

        images (:set_attribute_value (first (filter #(= "images" (:set_attribute_name %)) filters)))
        quantity (:set_attribute_value (first (filter #(= "quantity" (:set_attribute_name %)) filters)))
        db_quantity_res (dm/db_query_sender "" dm/product_storage_product_quantity_set_sql [{:product_id product_id :quantity quantity}])
        images-path (ad/app_data :images_path)
        ]
    
    (println "-----------------product_filter_attribute_add-----------------")
    (println filters)

    (println quantity)

    ;; Обработка изображений асинхронно с ограничением
    (when (and images product_id (seq images))
      (future  ;; Запускаем в отдельном потоке, не блокируем ответ
        (try
          (let [copy-tasks (->> images
                                (map #(process-single-image product_id % images-path))
                                (remove nil?)
                                (zipmap (range)))

                ;; Ждем завершения с таймаутом
                _ (wait-for-copies copy-tasks 30000) ;; 30 секунд таймаут

                successful-copies (->> copy-tasks
                                       (filter (fn [[_ task]]
                                                 (and (:future task)
                                                      (realized? (:future task))
                                                      (:success @(:future task)))))
                                       (map (fn [[_ task]] (:new-name task)))
                                       (remove nil?))]

            (when (seq successful-copies)
              (dm/db_query_sender "" dm/product_attribute_add_using_filter_sql
                                  [{:filters [{:attribute_name "product_id"
                                               :attribute_values [product_id]}]
                                    :set_attribute_name "images"
                                    :set_attribute_value successful-copies}])
              (println "Saved to DB:" successful-copies)))

          (catch Exception e
            (println "Error in image processing:" (.getMessage e))))))

    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}))

(defn third_lvl_get_config [req]
  (let [req_body (:params req)
        db_res (dm/db_query_sender "" dm/config_sysconfig_admin_3lvl_get_sql {})]
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)
     }
    )
  )

(defn product_add [req]
  (println "product_add")
  (let [req_body (:params req)
        common_attributes (:common_attributes req_body)
        db_add_res (dm/db_query_sender "" dm/product_add_sql {})
        println_res_1 (println db_add_res)
        product_id (str (:_product_id (first db_add_res))) 
        
        filters_data (into (mapv (fn [key]
                                   (let [attribute_name (name key)
                                         attribute_value (key common_attributes)]
                                     {:filters [{:attribute_name "product_id"
                                                 :attribute_values [product_id]}]
                                      :set_attribute_name attribute_name
                                      :set_attribute_value attribute_value}))
                                 (keys common_attributes)
                                 )
                           [{:filters [{:attribute_name "product_id"
                                        :attribute_values [product_id]}]
                             :set_attribute_name "actual"
                             :set_attribute_value "false"}])
        
        db_res (dm/db_query_sender "" dm/product_attribute_add_using_filter_sql filters_data)
        ]
    (println "---------")
    (println db_add_res)
    (println filters_data)
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str {:product_id product_id})
     }
    )
  )


(defn first_lvl_get_config [req]
  (let [req_body (:params req)
        db_res (dm/db_query_sender "" dm/config_sysconfig_admin_1lvl_get_sql {})]
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}))

(defn price_set [req]
  (let [req_body (:params req)
        prices (:prices req_body)
        db_res (dm/db_query_sender "" dm/price_set_sql prices)]
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}))



(defn picture_upload [req]
  (println "picture_upload")
  (println req)
  (let [params (:params req)
        product_id (get params "product_id")
        file-info (get params "file") 
        file-tempfile (:tempfile file-info)
        filename-from-file (:filename file-info)
        base-path (ad/app_data :images_path)
        db_res (dm/db_query_sender "" dm/product_get_image_name_sql {:product_id product_id :filename filename-from-file})
        filename (->> db_res first :_result :image_name)
        dest-path (str base-path "/" filename)
        dest-dir (java.io.File. base-path)]

    (println "File info:" file-info)
    (println "Product ID:" product_id)
    (println "Image name:" filename-from-file)


    (if (nil? file-tempfile)
      {:status 400
       :headers {"Content-Type" "application/json"}
       :body (json/write-str {:error "No file uploaded"})}

      (do
        (when-not (.exists dest-dir)
          (.mkdirs dest-dir))

        (clojure.java.io/copy file-tempfile (java.io.File. dest-path))

        (Thread/sleep 5000)

        {:status 200
         :headers {"Content-Type" "application/json"}
         :body (json/write-str {:filename filename})}
        )
      )
    )
    )


(defn product_image_delete [req]
  (let [req_body (:params req)
        image_name (:image_name req_body)
        images-dir (io/file (ad/app_data :images_path))
        image-file (io/file images-dir image_name)
        ]

    (when (and image_name
               (.exists image-file)
               (.isFile image-file))
      (try
        (io/delete-file image-file)
        (catch Exception e
          (println "Ошибка в удалении файла:" (.getMessage e)))))

    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str {:ok true})}
    )
  )





(defn banner_get [req]
  (let [req_body (:params req)
        banner_id (:banner_id req_body)
        banner_location (:banner_location req_body)
        banner_name (:banner_name req_body)
        date_start (:date_start req_body)
        date_end (:date_end req_body)
        db_res (dm/db_query_sender "" dm/banner_get_sql {:banner_id banner_id :banner_location banner_location :banner_name banner_name})]
    (println "banner_get")
    (println db_res)
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}))



(defn banner_add [req]
  (let [req_body (:params req)
        banner_id (:banner_id req_body)
        banner_location (:banner_location req_body)
        banner_name (:banner_name req_body)
        date_start (:date_start req_body)
        date_end (:date_end req_body)
        banner_images (:banner_images req_body)
        db_res (dm/db_query_sender "" dm/banner_add_sql {:banner_id banner_id :banner_location banner_location :banner_name banner_name :date_start date_start :date_end date_end :banner_images banner_images})]
    (println "banner_add")
    (println {:banner_id banner_id :banner_location banner_location :banner_name banner_name :date_start date_start :date_end date_end :banner_images banner_images})
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}))


(defn banner_picture_del [req]
  (let [req_body (:params req)
        image_name (:image_name req_body)
        images-dir (io/file (ad/app_data :banners_path))
        image-file (io/file images-dir image_name)]
  
    (when (and image_name
               (.exists image-file)
               (.isFile image-file))
      (try
        (io/delete-file image-file)
        (catch Exception e
          (println "Ошибка в удалении файла:" (.getMessage e)))))
  
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str {:ok true})})
  )


(defn banner_picture_upload [req]
  (println "banner_picture_upload")
  (println req)
  (let [params (:params req)
        file-info (get params "file")
        file-tempfile (:tempfile file-info)
        filename-from-file (:filename file-info)
        base-path (ad/app_data :banners_path)
        dest-path (str base-path "/" filename-from-file)
        dest-dir (java.io.File. base-path)]

    (println "File info:" file-info)
    (println "Image name:" filename-from-file)


    (if (nil? file-tempfile)
      {:status 400
       :headers {"Content-Type" "application/json"}
       :body (json/write-str {:error "No file uploaded"})}

      (do
        (when-not (.exists dest-dir)
          (.mkdirs dest-dir))

        (clojure.java.io/copy file-tempfile (java.io.File. dest-path))

        (Thread/sleep 5000)

        {:status 200
         :headers {"Content-Type" "application/json"}
         :body (json/write-str {:filename filename-from-file})}))))




(defn user_get_init [req]
  (println "user_get_init")
  (let [params (:params req)
        telegram_user_id (:telegram_user_id params)
        db_res (dm/db_query_sender "" dm/user_user_get_init_sql {:telegram_user_id telegram_user_id :app_type "admin"})]
    (println db_res)
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}))



(defn orders_history_get [req]
  (let [req_body (:params req)
        user_id_filter (:user_id_filter req_body)
        external_order_id_filter (:external_order_id_filter req_body)
        status_names_filter (:status_names_filter req_body)
        track_number_filter (:track_number_filter req_body)
        db_res (dm/db_query_sender "" dm/order_user_order_get_sql {:user_id_filter user_id_filter :external_order_id_filter external_order_id_filter :status_names_filter status_names_filter :track_number_filter track_number_filter})]
    (println "orders_history_get")
    (println db_res)
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}))



(defn order_get [req]
  (let [req_body (:params req)
        telegram_user_id (:telegram_user_id req_body)
        order_id (:order_id req_body)
        status_names (:status_names req_body)
        db_res (dm/db_query_sender "" dm/order_user_order_get_sql {:order_id order_id})
        data (first (vec (map (fn [product] (:data product)) db_res)))
        telegram_user_data (fetch-telegram-user-info (:telegram_user_id data))
        answer (assoc data :user_data telegram_user_data)
        ]
    (println "order_get")
    (println db_res)
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str [{:data answer}])}))


(defn statuses_get [req]
  (let [req_body (:params req)
        db_res (dm/db_query_sender "" dm/order_all_statuses_get_sql {})]
    (println "statuses_get")
    (println db_res)
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}))

(defn order_status_set [req]
  (let [req_body (:params req)
        order_id (:order_id req_body)
        status_name (:status_name req_body)
        db_res (dm/db_query_sender "" dm/order_order_status_set_sql [{:order_id order_id :status_name status_name}])
        order_res (dm/db_query_sender "" dm/order_user_order_get_sql {:order_id order_id})
        order_data (:data (first order_res))
        telegram_user_id (:telegram_user_id order_data)
        status_label (or (get-in order_data [:order_current_status :status_name_rus]) status_name)
        order_ref (or (:order_external_id order_data) (str order_id))
        catalog_bot_token (:catalog-bot-token api_keys)
        bot_name (fetch-bot-username catalog_bot_token)
        order_link (when (and bot_name order_id)
                     (order-startapp-link bot_name order_id))
        message (str "Статус заказа " order_ref " поменялся на "\" status_label "\""
                     (when order_link
                       (str "\nСсылка на заказ: " order_link)))
        ]
    (when (and catalog_bot_token telegram_user_id)
      (send-telegram-message-with-token catalog_bot_token telegram_user_id message nil {}))
    (println "statuses_get")
    (println db_res)
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}))

(defn order_track_number_set [req]
  (let [req_body (:params req)
        order_id (:order_id req_body)
        track_number (:track_number req_body)
        db_res (dm/db_query_sender "" dm/order_track_number_set_sql
                                   {:order_id order_id :track_number track_number})
        order_res (dm/db_query_sender "" dm/order_user_order_get_sql {:order_id order_id})
        order_data (:data (first order_res))
        telegram_user_id (:telegram_user_id order_data)
        order_ref (or (:order_external_id order_data) (str order_id))
        catalog_bot_token (:catalog-bot-token api_keys)
        message (str "Трек-номер заказа " order_ref " обновлён: " track_number)]
    (println "order_track_number_set")
    (println db_res)
    (when (and catalog_bot_token telegram_user_id)
      (send-telegram-message-with-token catalog_bot_token telegram_user_id message nil {}))
    {:status 200
     :headers {"Content-Type" "text/json"}
     :body (json/write-str db_res)}))



(defn managers_get [req]
  (println "managers_get")
  (let [req_body (:params req)
        db_res (dm/db_query_sender "" dm/user_managers_get_sql {}) 
        telegram_user_ids (mapv (fn [product] (:telegram_user_id (:data product))) db_res) 
        users_info (mapv (fn [telegram_user_id] (fetch-telegram-user-info telegram_user_id)) telegram_user_ids)
        ]
    (println "managers_get") 
    (println db_res)
    (println users_info)
    (println "------------------------------------")
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str users_info)
     }
    )
  )

(defn owners_get [req]
  (println "owners_get")
  (let [req_body (:params req)
        db_res (dm/db_query_sender "" dm/user_owners_get_sql {})
        telegram_user_ids (mapv (fn [product] (:telegram_user_id (:data product))) db_res)
        users_info (mapv (fn [telegram_user_id] (fetch-telegram-user-info telegram_user_id)) telegram_user_ids)]
    (println "owners_get")
    (println db_res)
    (println users_info)
    (println "------------------------------------")
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str users_info)}
    )
  )

(defn managers_request_get [req]
  
  (let [req_body (:params req)
        db_res (dm/db_query_sender "" dm/user_user_status_request_get_sql {})
        telegram_user_ids (mapv (fn [product] (:telegram_user_id (:data product))) db_res)
        users_info (mapv (fn [telegram_user_id] (fetch-telegram-user-info telegram_user_id)) telegram_user_ids)]
    (println "managers_request_get")
    (println db_res)
    (println users_info)
    (println "------------------------------------")
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str users_info)}
    )
  )


(defn managers_request_del [req]
  (let [req_body (:params req)
        manager_user_id (:manager_user_id req_body)
        db_res (dm/db_query_sender "" dm/user_user_status_request_delete_sql {:telegram_user_id manager_user_id})
        ]
    (println "managers_request_get")
    (println db_res)
    (println "------------------------------------")
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}))



(defn managers_set [req]
  (let [req_body (:params req) 
        managers_data (:managers_data req_body)
        db_res (dm/db_query_sender "" dm/user_user_status_set_sql managers_data)
        ]
    (doseq [user managers_data]
      (dm/db_query_sender "" dm/user_user_status_request_delete_sql {:telegram_user_id (:telegram_user_id user)})
      )
    (println "managers_set")
    (println db_res)
    (println "------------------------------------")
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}
    )
  )


(defn telegram-webhook [req]
  (println "=== Входящий запрос ===")
  (println req)
  (let [body (:params req)
        update-id (:update_id body)
        pre-checkout-query (:pre_checkout_query body)
        message (:message body)
        callback-query (:callback_query body)]

    (when (and message (:text message))
      (let [text (:text message)
            chat-id (get-in message [:chat :id])
            from-user (:from message)]

        (when (.startsWith text "/start")
          (println "Получена команда /start от пользователя:" from-user)
          (println "Chat ID:" chat-id)

          (send-telegram-message chat-id (str "вы можете запускать приложение как в этом боте в Telegram, так и в браузере по ссылке: " (:admin_url ad/app_data)))

          (dm/db_query_sender "" dm/user_user_add_sql {:telegram_user_id (:id from-user)}) 
          )
        
        (when (.startsWith text "/request")
          (println "Запрос на менеджера от:" from-user)
          (println "Chat ID:" chat-id)
        
          (dm/db_query_sender "" dm/user_user_status_request_create_sql {:telegram_user_id (:id from-user) :status_name "manager"})
          )
        )
      )
    
    {:status 200 :body "OK"}))


(defn settings_set [req]
  (println "settings_set")
  (let [params (:params req)
        config_name (:config_name params)
        config_value (:config_value params)
        db_res (dm/db_query_sender "" dm/config_system_config_set_sql {:config_name config_name :config_value config_value})]
    (println db_res)
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}))



(defn settings_get [req]
  (println "settings_get")
  (let [params (:params req)
        db_res (dm/db_query_sender "" dm/config_system_config_get_sql {})
        bot-token (or (:catalog-bot-token api_keys) (:bot-token api_keys))
        telegram-description (fetch-telegram-bot-description bot-token)
        db_res (if (and telegram-description (seq db_res))
                 (mapv (fn [row]
                         (if-let [payload (:_r row)]
                           (assoc row :_r (assoc-in payload [:telegram_bot :description] telegram-description))
                           row))
                       db_res)
                 db_res)]
    (println db_res)
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}))

(defn store_get [req]
  (println "store_get")
  (let [db_res (dm/db_query_sender "" dm/stores_stores_get_sql {})]
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}))

(defn catalog_bot_description_set [req]
  (println "catalog_bot_description_set")
  (let [params (:params req)
        description (:description params)
        bot-token (or (:catalog-bot-token api_keys) (:bot-token api_keys))
        result (if bot-token
                 (set-telegram-bot-description bot-token description)
                 {:ok false :error "Не найден bot-token для бота каталога"})]
    (println "catalog_bot_description_set params:" params)
    (println "description:" description)
    (println "bot-token present:" (boolean bot-token))
    (when (some? description)
      (let [db-res (dm/db_query_sender "" dm/config_system_config_set_sql
                                       {:config_name "catalog_bot_description"
                                        :config_value description})]
        (println "db catalog_bot_description set:" db-res)))
    (println "catalog_bot_description_set result:" result)
    {:status  200
     :headers {"Content-Type" "application/json"}
     :body    (json/write-str result)}))






(defn payment_provider_get [req]
  (println "payment_provider_get")
  (let [params (:params req)
        db_res (dm/db_query_sender "" dm/order_payment_provider_get_sql {})]
    (println db_res)
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)
     }
    )
  )


(defn payment_provider_add [req]
  (println "payment_provider_add")
  (let [params (:params req)
        payment_provider_name (:payment_provider_name params)
        connection_attributes (:connection_attributes params)
        db_res (dm/db_query_sender "" dm/order_payment_provider_add_sql {:payment_provider_name payment_provider_name :connection_attributes connection_attributes})]
    (println params)
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)
     }
    )
  )


(defn payment_provider_del [req]
  (println "payment_provider_del")
  (let [params (:params req)
        payment_provider_name (:payment_provider_name params)
        db_res (dm/db_query_sender "" dm/order_payment_provider_del_sql {:payment_provider_name payment_provider_name})]
    (println db_res)
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)
     }
    )
  )

(defn payment_provider_validate [req]
  (println "payment_provider_validate")
  (let [params (:params req)
        payment_provider_name (:payment_provider_name params)
        provider_token (:provider_token params)
        bot-token (:catalog-bot-token api_keys)]
    (if (not= "youkassa" payment_provider_name)
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body (json/write-str {:ok true})}
      (if (or (nil? provider_token) (s/blank? (str provider_token)))
        {:status 200
         :headers {"Content-Type" "application/json"}
         :body (json/write-str {:ok false :error "Введите токен Юкассы"})}
        (if (nil? bot-token)
          {:status 200
           :headers {"Content-Type" "application/json"}
           :body (json/write-str {:ok false :error "Не найден bot-token для проверки"})}
          (let [payload {:title "Проверка провайдера"
                         :description "Проверка токена оплаты"
                         :payload (str "validate_" (System/currentTimeMillis))
                         :provider_token provider_token
                         :currency "RUB"
                         :prices [{:label "Проверка" :amount 10000}]
                         :need_name false
                         :need_email false
                         :need_phone_number false}
                response (http/post (str "https://api.telegram.org/bot"
                                         bot-token
                                         "/createInvoiceLink")
                                    {:content-type :json
                                     :body (json/write-str payload)
                                     :throw-exceptions false})
                result (when-let [body (:body response)]
                         (json/read-str body :key-fn keyword))]
            (if (and result (:ok result))
              {:status 200
               :headers {"Content-Type" "application/json"}
               :body (json/write-str {:ok true})}
              {:status 200
               :headers {"Content-Type" "application/json"}
               :body (json/write-str {:ok false
                                      :error "Неверный токен Юкассы"})})))))))








(defn delivery_provider_get [req]
  (println "delivery_provider_get")
  (let [params (:params req)
        db_res (dm/db_query_sender "" dm/order_delivery_provider_get_sql {})]
    (println db_res)
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}))


(defn delivery_provider_add [req]
  (println "delivery_provider_add")
  (let [params (:params req)
        delivery_provider_name (:delivery_provider_name params)
        connection_attributes (:connection_attributes params)
        db_res (dm/db_query_sender "" dm/order_delivery_provider_add_sql {:delivery_provider_name delivery_provider_name :connection_attributes connection_attributes})]
    (println params)
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}))


(defn delivery_provider_del [req]
  (println "delivery_provider_del")
  (let [params (:params req)
        delivery_provider_name (:delivery_provider_name params)
        db_res (dm/db_query_sender "" dm/order_delivery_provider_del_sql {:delivery_provider_name delivery_provider_name})]
    (println db_res)
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}))


(defroutes api-routes
  ;общие функции
  (POST  "/product-filter-attribute-add"    []  product_filter_attribute_add) 
  (POST  "/price-set"                       []  price_set)
  (POST  "/picture-upload"                  []  picture_upload)
  (POST  "/user-get-init"                   []  user_get_init)
  ;1 уровень
  (POST  "/first-lvl-get-config"            []  first_lvl_get_config) 
  (POST  "/units-get"                       []  admin_product_get_unit) 
  (POST  "/filters-get"                     []  admin_product_attribute_get_filter)
  (POST  "/archive-product"                 []  admin_product_to_archive) 
  (POST  "/banner-get"                      []  banner_get)
  (POST  "/banner-add"                      []  banner_add)
  (POST  "/banner-picture-del"              []  banner_picture_del)
  (POST  "/banner-picture-upload"           []  banner_picture_upload)

  ;2 уровень
  (POST  "/model-unit-get"                  []  product_get_one_per_row) 
  (POST  "/second-lvl-get-config"           []  second_lvl_get_config)  
  ;3 уровень
  (POST  "/third-lvl-get-config"            []  third_lvl_get_config) 
  (POST  "/product-get"                     []  product_get_one_per_row)
  (POST  "/product-add"                     []  product_add) 
  (POST  "/product-image-delete"            []  product_image_delete) 
  ;история заказов
  (POST  "/orders-history-get"              []  orders_history_get)
  (POST  "/order-get"                       []  order_get)
  (POST  "/statuses-get"                    []  statuses_get)
  ;страница заказа
  (POST  "/order-status-set"                []  order_status_set)
  (POST  "/order-track-number-set"          []  order_track_number_set)
  ;страница менеджеров
  (POST  "/managers-get"                    []  managers_get)
  (POST  "/owners-get"                      []  owners_get)
  (POST  "/managers-request-get"            []  managers_request_get)
  (POST  "/managers-request-del"            []  managers_request_del)
  (POST  "/managers-set"                    []  managers_set)
  ;настройки приложения
  (POST  "/settings-set"                    []  settings_set)
  (POST  "/settings-get"                    []  settings_get)
  (POST  "/catalog-bot-description-set"     []  catalog_bot_description_set)
  (POST  "/payment-provider-get"            []  payment_provider_get)
  (POST  "/payment-provider-add"            []  payment_provider_add)
  (POST  "/payment-provider-del"            []  payment_provider_del)
  (POST  "/payment-provider-validate"       []  payment_provider_validate)
  (POST  "/delivery-provider-get"            [] delivery_provider_get)
  (POST  "/delivery-provider-add"            [] delivery_provider_add)
  (POST  "/delivery-provider-del"            [] delivery_provider_del)
  (POST  "/cdek-search-city"                []  cdek/cdek-search-city)
  (POST  "/cdek-delivery-points"            []  cdek/cdek-delivery-points)
  (POST  "/store-get"                       []  store_get)
  )

(defroutes webhook-routes
  (POST  "/telegram"                        []  telegram-webhook)
  )

(defroutes app-routes
  (context "/api" []
    (-> api-routes
        (auth/wrap-telegram-auth (:bot-token api_keys))
        )
    )

  (context "/webhook" [] webhook-routes)

  (route/not-found "There is no route you are looking for"))






(def app (-> app-routes 
             wrap-multipart-params
             wrap-params
             wrap-keyword-params 
             wrap-json-params 
             (wrap-cors :access-control-allow-origin [#".*"]
                        :access-control-allow-methods [:get :post :put :delete :options])
             )
  )

(defn -main [& args]
  (server/run-server app {:port (ad/app_data :port)
                          :max-body 1000000000
                          :max-ws 1000000000
                          :max-line 1000000000
                          :timeout 3600000}) 
  (println (str "Server started on port " (ad/app_data :port))))
