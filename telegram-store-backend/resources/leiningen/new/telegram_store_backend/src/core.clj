(ns {{name}}.core
  (:require
   [org.httpkit.server :as server]
   [ring.middleware.cors :refer [wrap-cors]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
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
   [{{name}}.services.cdek :as cdek]
   [{{name}}.services.youkassa :as youkassa]
   [{{name}}.tg-auth :as auth]
   )
  (:import [java.util.zip GZIPInputStream]
           [java.io ByteArrayInputStream ByteArrayOutputStream]
           (javax.crypto Mac)
           (javax.crypto.spec SecretKeySpec)
           [java.util.concurrent Executors TimeUnit])
  (:gen-class)
  )

(def api_keys (ad/app_data :api_keys))

(declare notify-order-created)


(defn products_get [req]
  (let [req_body (:params req)
        telegram_user_id (:telegram_user_id req_body)
        filters (:filters req_body)
        search_string (:search_string req_body)
        order_by (:order_by req_body)
        db_res (dm/db_query_sender "" dm/ui_product_get_list_sql  {:telegram_user_id telegram_user_id :search_string search_string :filters filters :order_by order_by})]
    (println "product_get")
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}))



(defn cart_get [req]
  (let [req_body (:params req)
        telegram_user_id (:telegram_user_id req_body)
        db_res (dm/db_query_sender "" dm/user_cart_get_sql {:telegram_user_id telegram_user_id})]
    (println "cart_get")
    (println req)
    (println req_body)
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}))

(defn cart_set [req]
  (let [req_body (:params req)
        telegram_user_id (:telegram_user_id req_body)
        quantity (:quantity req_body)
        product_id (:product_id req_body)
        db_res (dm/db_query_sender "" dm/user_cart_set_sql {:telegram_user_id telegram_user_id :quantity quantity :product_id product_id})
        cart_res (dm/db_query_sender "" dm/user_cart_get_sql {:telegram_user_id telegram_user_id})
        summary_res (dm/db_query_sender "" dm/user_cart_get_cummary_sql {:telegram_user_id telegram_user_id})]
    (println "cart_set")
    (println db_res)
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str {:cart_set db_res
                               :cart_get cart_res
                               :cart_get_summary summary_res})}))


(defn cart_get_summary [req]
  (let [req_body (:params req)
        telegram_user_id (:telegram_user_id req_body)
        db_res (dm/db_query_sender "" dm/user_cart_get_cummary_sql {:telegram_user_id telegram_user_id})]
    (println "cart_get_summary")
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}))

 
(defn normalize-favorites [favorites telegram_user_id]
  (let [items (cond
                (nil? favorites) []
                (map? favorites) [favorites]
                (sequential? favorites) favorites
                :else [])]
    (->> items
         (filter map?)
         (mapv (fn [item]
                 (if (and (contains? item :telegram_user_id)
                          (some? (:telegram_user_id item)))
                   item
                   (assoc item :telegram_user_id telegram_user_id)))))))

(defn favorite_get [req]
  (let [req_body (:params req)
        telegram_user_id (:telegram_user_id req_body)
        db_res (dm/db_query_sender "" dm/user_favorite_get_sql {:telegram_user_id telegram_user_id})]
    (println "favorite_get")
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}))

(defn favorite_add [req]
  (let [req_body (:params req)
        telegram_user_id (:telegram_user_id req_body)
        favorites (normalize-favorites (:favorites req_body) telegram_user_id)
        db_res (dm/db_query_sender "" dm/user_favorite_add_sql favorites)]
    (println "favorite_add")
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}))

(defn favorite_del [req]
  (let [req_body (:params req)
        telegram_user_id (:telegram_user_id req_body)
        favorites (normalize-favorites (:favorites req_body) telegram_user_id)
        db_res (dm/db_query_sender "" dm/user_favorite_del_sql favorites)]
    (println "favorite_del")
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}))


(defn user_add [req]
  (let [req_body (:params req)
        telegram_user_id (:telegram_user_id req_body)
        users_attributes (:users_attributes req_body)
        params (if users_attributes
                 {:telegram_user_id telegram_user_id
                  :users_attributes users_attributes}
                 {:telegram_user_id telegram_user_id})
        db_res (dm/db_query_sender "" dm/user_user_add_sql params)]
    (println "user_add")
    (println "users_attributes:" users_attributes)
    (println db_res)
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}))


(defn policies_get [req]
  (let [req_body (:params req)
        telegram_user_id (:telegram_user_id req_body)
        db_res (dm/db_query_sender "" dm/user_policies_get_sql {:telegram_user_id telegram_user_id})]
    (println "policies_get")
    (println db_res)
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}))


(defn policies_get_all [req]
  (let [req_body (:params req)
        telegram_user_id (:telegram_user_id req_body)
        db_res (dm/db_query_sender "" dm/config_policies_get_all_sql {})]
    (println "policies_get_all")
    (println db_res)
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}))


(defn user_get_init [req]
  (println "user_get_init")
  (let [params (:params req)
        telegram_user_id (:telegram_user_id params)
        db_res (dm/db_query_sender "" dm/user_user_get_init_sql {:telegram_user_id telegram_user_id :app_type "catalog"})]
    (println db_res)
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}))


(defn catalog_config_get [req]
  (println "catalog_config_get")
  (let [params (:params req)
        db_res (dm/db_query_sender "" dm/config_sysconfig_catalog_get_sql {})]
    (println db_res)
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}))

(defn settings_get [req]
  (println "settings_get")
  (let [params (:params req)
        db_res (dm/db_query_sender "" dm/config_system_config_get_sql {})]
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


(defn filters_get [req]
  (let [req_body (:params req)
        telegram_user_id (:telegram_user_id req_body)
        db_res (dm/db_query_sender "" dm/product_product_attribute_get_filter {:telegram_user_id telegram_user_id})]
    (println "filters_get")
    (println db_res)
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}))


(defn unit_get [req]
  (println "unit_get")
  (let [req_body (:params req)
        telegram_user_id (:telegram_user_id req_body)
        unit_id (:unit_id req_body)
        db_res (dm/db_query_sender "" dm/ui_product_get_single_unit {:unit_id unit_id})] 
    (println db_res)
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}))


(defn product_get_config [req]
  (println "product_get_config")
  (let [req_body (:params req)
        telegram_user_id (:telegram_user_id req_body)
        db_res (dm/db_query_sender "" dm/config_sysconfig_unit_get {})]
    (println db_res)
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}))

(defn product_get [req]
  (println "product_get")
  (let [req_body (:params req)
        product_id (:product_id req_body)
        db_res (dm/db_query_sender "" dm/product_get_one_per_row_sql {:filters [{:attribute_name "product_id"
                                                                                 :attribute_values [product_id]
                                                                                 }
                                                                                ]
                                                                      }
                                   )]
    (println db_res)
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}
    )
  )


(defn orders_history_get [req]
  (let [req_body (:params req)
        telegram_user_id (:telegram_user_id req_body)
        order_id (:order_id req_body)
        status_names (:status_names req_body)
        db_res (dm/db_query_sender "" dm/order_user_order_get_sql {:user_id_filter [telegram_user_id] :order_id order_id :status_names status_names})]
    (println "orders_history_get")
    (println db_res)
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}))



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

;; CDEK order draft helpers (no order creation here).
(def cdek-tariff-info
  {136 {:from-type :pvz :to-type :pvz}
   137 {:from-type :pvz :to-type :door}
   138 {:from-type :door :to-type :pvz}
   139 {:from-type :door :to-type :door}})

(defn shipping-attr [shipping key]
  (let [string-key (name key)]
    (or (get shipping key)
        (get shipping string-key))))

(defn connection-attr [attrs key]
  (let [string-key (name key)]
    (or (get attrs key)
        (get attrs string-key))))

(defn nonblank? [value]
  (and (string? value) (not (s/blank? value))))

(defn join-nonblank [parts]
  (->> parts
       (map #(if (string? %) (s/trim %) %))
       (filter nonblank?)
       (s/join " ")))

(defn normalize-code [value]
  (when (some? value)
    (let [sval (str value)]
      (when-not (s/blank? sval) sval))))

(defn cdek-provider-attrs []
  (let [{:keys [provider]} (cdek/cdek-credentials)]
    (or (:connection_attributes provider) {})))

(defn- parse-number [value]
  (cond
    (number? value) (double value)
    (string? value) (try
                      (Double/parseDouble (s/replace value #"," "."))
                      (catch Exception _ nil))
    :else nil))

(defn- normalize-int [value default-val]
  (let [num (parse-number value)
        intval (when (some? num) (long (Math/round num)))]
    (if (and intval (pos? intval)) intval default-val)))

(defn- cart-item-attrs [item]
  (or (:product_attributes item)
      (get item "product_attributes")
      {}))

(defn- cart-item->cdek-item [idx item]
  (let [attrs (cart-item-attrs item)
        name (or (:product_name attrs)
                 (:product_name item)
                 (:name item)
                 "Товар")
        ware (or (:vendor_code attrs)
                 (:vendor_code item)
                 (:product_id attrs)
                 (:product_id item)
                 (str "item-" (inc idx)))
        quantity (normalize-int (:quantity item) 1)
        total (or (parse-number (:discount_final_summ item))
                  (parse-number (:final_summ item))
                  (parse-number (:discount_summ item))
                  (parse-number (:summ item)))
        unit-cost (if (and total (pos? quantity))
                    (long (Math/round (/ total quantity)))
                    100)
        weight 1000]
    {:name (str name)
     :ware_key (str ware)
     :cost unit-cost
     :amount quantity
     :weight weight
     :payment {:value unit-cost}}))

(defn- cart->cdek-items [cart]
  (->> (or cart [])
       (map-indexed cart-item->cdek-item)
       (filter map?)
       vec))

(declare ensure-packages)

(defn- attach-items-to-packages [packages items]
  (let [packages (ensure-packages packages)]
    (if (seq items)
      (let [first-pkg (assoc (first packages) :items items)]
        (vec (cons first-pkg (rest packages))))
      packages)))

(defn- default-cdek-item [weight]
  {:name "Товар"
   :ware_key "item-1"
   :cost 100
   :weight weight
   :amount 1
   :payment {:value 100}})

(defn- normalize-package [idx pkg]
  (let [pkg (if (map? pkg) pkg {})
        weight (or (:weight pkg) (get pkg "weight") 1000)
        number (or (:number pkg) (get pkg "number") (str (inc idx)))
        items (or (:items pkg) (get pkg "items"))
        items (if (seq items) items [(default-cdek-item weight)])]
    (assoc pkg :weight weight :number number :items items)))

(defn ensure-packages [packages]
  (let [normalized (->> (if (seq packages) packages [nil])
                        (map-indexed normalize-package)
                        vec)]
    (if (seq normalized) normalized [{:weight 1000 :number "1" :items [(default-cdek-item 1000)]}])))

(defn- safe-cdek-order-create [params context]
  (try
    (cdek/cdek-order-create {:params params})
    (catch Exception e
      (println "[CDEK] order_create failed:" context)
      (println "[CDEK] order_create params:" (pr-str params))
      (println "[CDEK] order_create error:" (.getMessage e))
      nil)))

(defn- cdek-body [resp]
  (let [body (:body resp)]
    (cond
      (map? body) body
      (string? body) (try
                       (json/read-str body :key-fn keyword)
                       (catch Exception _ {}))
      :else {})))

(defn- cdek-order-uuid [resp]
  (let [outer (cdek-body resp)
        body (if (map? (:body outer)) (:body outer) outer)
        entity (:entity body)]
    (or (:uuid entity) (:uuid body))))

(defn- cdek-track-number [resp]
  (let [outer (cdek-body resp)
        body (if (map? (:body outer)) (:body outer) outer)
        entity (:entity body)
        related (or (:related_entities body) [])
        order-entity (first (filter #(= "order" (:type %)) related))]
    (or (:cdek_number order-entity)
        (:order_number order-entity)
        (:number order-entity)
        (:cdek_number entity)
        (:order_number entity)
        (:number entity)
        (:cdek_number body)
        (:order_number body)
        (:number body))))

(defn- set-order-track-number! [order-id track-number]
  (when (and order-id (some? track-number))
    (dm/db_query_sender "" dm/order_track_number_set_sql
                        {:order_id order-id :track_number (str track-number)})))

(defn- cdek-order-info [order-uuid]
  (cdek/cdek-order-info {:params {:uuid order-uuid}}))

(defn- cdek-log-history! [history-id order-create-resp order-info-resp track-number order-uuid]
  (when history-id
    (cdek/cdek-history-upd!
     history-id
     {:order_create {:status (:status order-create-resp)
                     :body (cdek-body order-create-resp)}
      :order_info {:status (:status order-info-resp)
                   :body (cdek-body order-info-resp)}
      :order_uuid order-uuid
      :track_number (when (some? track-number) (str track-number))})))

(defn- fetch-track-number! [order-id order-create-resp history-id telegram-user-id]
  (let [order-uuid (cdek-order-uuid order-create-resp)]
    (if-not order-uuid
      (println "[CDEK] order_info skipped: no uuid for order_id=" order-id)
      (do
        (println "[CDEK] order_info start"
                 "order_id=" order-id
                 "uuid=" order-uuid)
        (let [order-info-resp (or (cdek-order-info order-uuid)
                                  {:status 500 :body {}})
              track (cdek-track-number order-info-resp)]
          (if track
            (do
              (set-order-track-number! order-id track)
              (cdek-log-history! history-id order-create-resp order-info-resp track order-uuid)
              (println "[CDEK] order_info done - track number received immediately:"
                       "order_id=" order-id
                       "uuid=" order-uuid
                       "track_number=" track))
            (do
              (cdek-log-history! history-id order-create-resp order-info-resp nil order-uuid)
              (println "[CDEK] order_info done - no track number yet, scheduling retries"
                       "order_id=" order-id
                       "uuid=" order-uuid)
              (cdek/schedule-track-number-retries order-id order-uuid history-id))))))))

(defn build-cdek-order-draft [shipping payment-id]
  (let [tariff-code (or (cdek/parse-int-safe (shipping-attr shipping :cdek_tariff_code))
                        (cdek/parse-int-safe (shipping-attr shipping :tariff_code)))
        {:keys [from-type to-type]} (get cdek-tariff-info tariff-code {})
        attrs (cdek-provider-attrs)
        from-city-code (or (connection-attr attrs :from_city_code)
                           (shipping-attr shipping :from_city_code))
        from-pvz-code (or (connection-attr attrs :from_pvz_code)
                          (shipping-attr shipping :from_pvz_code))
        from-address (or (connection-attr attrs :from_address)
                         (shipping-attr shipping :from_address))
        to-city-code (shipping-attr shipping :cdek_city_code)
        to-pvz-code (shipping-attr shipping :cdek_pvz_code)
        to-address (shipping-attr shipping :address)
        comment (shipping-attr shipping :comment)
        recipient-name (join-nonblank [(shipping-attr shipping :surname)
                                       (shipping-attr shipping :first_name)
                                       (shipping-attr shipping :patronymic)])
        phone (shipping-attr shipping :phone)
        email (shipping-attr shipping :mail)
        packages (ensure-packages (or (shipping-attr shipping :packages)
                                      (get shipping "packages")))
        base (cond-> {:tariff_code tariff-code
                      :recipient (cond-> {}
                                   (nonblank? recipient-name) (assoc :name recipient-name)
                                   (nonblank? phone) (assoc :phones [{:number phone}])
                                   (nonblank? email) (assoc :email email))
                      :packages packages}
               (nonblank? comment) (assoc :comment comment)
               payment-id (assoc :number (str payment-id)))
        base (cond-> base
               (and (= from-type :pvz) (normalize-code from-pvz-code))
               (assoc :shipment_point (normalize-code from-pvz-code))
               (and (= from-type :door) (or (normalize-code from-city-code)
                                            (nonblank? from-address)))
               (assoc :from_location (cond-> {}
                                       (normalize-code from-city-code) (assoc :code (normalize-code from-city-code))
                                       (nonblank? from-address) (assoc :address from-address)))
               (and (= to-type :pvz) (normalize-code to-pvz-code))
               (assoc :delivery_point (normalize-code to-pvz-code))
               (and (= to-type :door) (or (normalize-code to-city-code)
                                          (nonblank? to-address)))
               (assoc :to_location (cond-> {}
                                     (normalize-code to-city-code) (assoc :code (normalize-code to-city-code))
                                     (nonblank? to-address) (assoc :address to-address))))]
    base))



(defn payment_add [req]
  (let [req_body (:params req)
        telegram_user_id (:telegram_user_id req_body)
        payment_type (:payment_type req_body)
        delivery_cost (:delivery_cost req_body)
        delivery_provider_name (:delivery_provider_name req_body)
        history_fields (let [raw (:history_fields req_body)]
                         (if (map? raw) raw {}))
        delivery_attributes (let [raw (:delivery_attributes req_body)]
                              (if (map? raw) raw {}))
        cart (mapv (fn [product] (:data product))
                   (dm/db_query_sender "" dm/user_cart_get_sql {:telegram_user_id telegram_user_id}))
        cdek-items (cart->cdek-items cart)
        cdek-draft (when (= delivery_provider_name "cdek")
                     (let [existing (or (:cdek_order_draft delivery_attributes)
                                        (get delivery_attributes "cdek_order_draft"))
                           base (cond
                                  (map? existing) (update existing :packages ensure-packages)
                                  :else (build-cdek-order-draft delivery_attributes nil))]
                       (when (map? base)
                         (update base :packages #(attach-items-to-packages % cdek-items)))))
        delivery_attributes (cond-> delivery_attributes
                              cdek-draft (assoc :cdek_order_draft cdek-draft))
        db_res (dm/db_query_sender "" dm/order_payments_add_sql {:telegram_user_id telegram_user_id
                                                                 :payment_provider_name payment_type
                                                                 :delivery_cost delivery_cost
                                                                 :delivery_provider_name delivery_provider_name
                                                                 :delivery_attributes delivery_attributes
                                                                 :history_fields history_fields})


        payment_id (-> db_res first :payments_add :payment_id)
        payment_providers (mapv (fn [product] (:data product)) (dm/db_query_sender "" dm/order_payment_provider_get_sql {}))
        provider (some (fn [item]
                         (when (= (:payment_provider_name item) payment_type)
                           item))
                       payment_providers
                       )

        _ (dm/db_query_sender "" dm/exchange_history_add_sql
                              {:service_type "order_form"
                               :request history_fields
                               :context {:telegram_user_id telegram_user_id
                                         :payment_id payment_id
                                         }
                               :history_id (str payment_id)})
        ]

    (println "payment_add")
    (println db_res)
    (println payment_id)
    (println payment_type)
    (println payment_providers)
    (println provider)


    (case payment_type
      "youkassa" (let [payment-link (youkassa/create-telegram-invoice-link payment_id
                                                                  cart
                                                                  (get-in provider [:connection_attributes :provider_token])
                                                                  delivery_cost
                                                                  telegram_user_id
                                                                  delivery_provider_name)]
                   (if payment-link
                     {:status 200
                      :headers {"Content-Type" "application/json"}
                      :body (json/write-str {:payment_link payment-link
                                             :payment_id payment_id})}
                     {:status 500
                      :headers {"Content-Type" "application/json"}
                      :body (json/write-str {:error "Failed to create payment link"})}
                     )
                   ) 
      "default" (let [db_complete (dm/db_query_sender "" dm/user_cart_set_complete_sql {:telegram_user_id telegram_user_id
                                                                                       :payment_id payment_id})
                      order_id (-> db_complete first :cart_set_complete :order_id)]
                  (println "---------------------default---------------------")
                  (println db_complete)

                  (when order_id
                    (notify-order-created order_id telegram_user_id))
                  (when (and order_id (= delivery_provider_name "cdek"))
                    (let [cdek-request (get-in delivery_attributes [:cdek_order_draft])]
                      (println "[CDEK] order_create start (payment_add)")
                      (println "[CDEK] order_id:" order_id)
                      (println "[CDEK] payment_id:" payment_id)
                      (println "[CDEK] cdek_request:" (pr-str cdek-request))
                      (when (seq cdek-request)
                        (let [resp (safe-cdek-order-create {:telegram_user_id telegram_user_id
                                                            :history_id (str payment_id)
                                                            :order_id order_id
                                                            :payment_id payment_id
                                                            :delivery_provider_name delivery_provider_name
                                                            :cdek_request cdek-request}
                                                           :payment_add)]
                          (fetch-track-number! order_id resp (str payment_id) telegram_user_id)))))
                  {:status 200
                   :headers {"Content-Type" "application/json"}
                   :body (json/write-str {:payment_id payment_id
                                          :order_id order_id})})
      ) 
    )
    )

(defn delivery_provider_get [req]
  (println "delivery_provider_get")
  (let [db_res (dm/db_query_sender "" dm/order_delivery_provider_get_sql {})]
    (println db_res)
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}
    )
  )


(defn answer-pre-checkout-query [query-id ok & [error-message]]
  (let [url (str "https://api.telegram.org/bot" (:bot-token api_keys) "/answerPreCheckoutQuery")
        params {:pre_checkout_query_id query-id
                :ok ok
                :error_message error-message}
        res (http/post url {:form-params params
                            :content-type :json})]
    (println (str "answer-pre-checkout-query = " res))
    )
  )

(defn send-telegram-message [bot-token chat-id text]
  (let [url (str "https://api.telegram.org/bot" bot-token "/sendMessage")
        params {:chat_id chat-id
                :text text}
        res (http/post url {:form-params params
                            :content-type :json})]
    (println (str "send-telegram-message = " res))
    )
  )

(def bot-username-cache (atom {}))

(defn get-bot-username [bot-token]
  (if-let [cached (get @bot-username-cache bot-token)]
    cached
    (let [url (str "https://api.telegram.org/bot" bot-token "/getMe")
          res (http/get url {:as :text})
          result (json/read-str (:body res) :key-fn keyword)
          username (get-in result [:result :username])]
      (when username
        (swap! bot-username-cache assoc bot-token username))
      username)))

(defn base64url-encode [s]
  (let [encoded (String. (base64/encode (.getBytes s "UTF-8")) "UTF-8")]
    (-> encoded
        (s/replace "+" "-")
        (s/replace "/" "_"))))

(defn order-startapp-link [bot-token order-id]
  (when-let [bot-name (get-bot-username bot-token)]
    (let [payload (json/write-str {:type "order" :order_id (str order-id)})
          encoded (base64url-encode payload)]
      (str "https://t.me/" bot-name "?startapp=" encoded))))

(defn notify-order-created [order-id buyer-chat-id]
  (let [db_managers_res (dm/db_query_sender "" dm/user_managers_get_sql {})
        managers_user_ids (mapv (fn [product] (:telegram_user_id (:data product))) db_managers_res)
        db_owners_res (dm/db_query_sender "" dm/user_owners_get_sql {})
        owners_user_ids (mapv (fn [product] (:telegram_user_id (:data product))) db_owners_res)
        order_link (str (ad/app_data :admin_url) "/#/order/" order-id)
        recipients (distinct (concat managers_user_ids owners_user_ids))
        admin-bot-token (:bot-admin-token api_keys)
        message_text (str "Новый заказ: " order_link)
        buyer-link (order-startapp-link (:bot-token api_keys) order-id)
        buyer-message (when buyer-link (str "Вы успешно оформили заказ: " buyer-link))]
    (when admin-bot-token
      (doseq [user-id recipients]
        (send-telegram-message admin-bot-token user-id message_text)))
    (when (and buyer-message buyer-chat-id)
      (send-telegram-message (:bot-token api_keys) buyer-chat-id buyer-message))))



(defn telegram-webhook [req]
  (println "=== Входящий запрос ===")
  (println req)
  (let [body (:params req)
        update-id (:update_id body)
        pre-checkout-query (:pre_checkout_query body)
        message (:message body)
        callback-query (:callback_query body)
        ]

    (when (and message (:text message))
      (let [text (:text message)
            chat-id (get-in message [:chat :id])
            from-user (:from message)]

        (when (.startsWith text "/start")
          (println "Получена команда /start от пользователя:" from-user)
          (println "Chat ID:" chat-id)

          (let [start-param (when (> (count text) 7)
                              (s/trim (subs text 7)))
                user-params (if (and start-param (not (s/blank? start-param)))
                              {:telegram_user_id (:id from-user)
                               :users_attributes [{:attribute_name "startbot"
                                                   :attribute_value start-param
                                                   :update_existing false}]}
                              {:telegram_user_id (:id from-user)})]
            (println "[Bot] Start param:" start-param)
            (dm/db_query_sender "" dm/user_user_add_sql user-params))

          (let [settings (some-> (dm/db_query_sender "" dm/config_system_config_get_sql {})
                                 first
                                 :_r)
                start-message (or (:start_message settings)
                                  (get-in settings [:telegram_bot :start_message])
                                  (get-in settings [:telegram_bot :welcome_message]))]
            (when (and (string? start-message)
                       (not (s/blank? start-message)))
              (send-telegram-message (:bot-token api_keys) chat-id start-message))))
          )
        ) 

    (when pre-checkout-query
      (let [query-id (:id pre-checkout-query)
            currency (:currency pre-checkout-query)
            total-amount (:total_amount pre-checkout-query)]
        (println (str "pre-checkout-query = " query-id))
        (answer-pre-checkout-query query-id true) 
        )
      )

    (when (:successful_payment message)
      (let [chat (:chat message)
            successful-payment (:successful_payment message)
            invoice-payload (:invoice_payload successful-payment)
            payment-id (when (string? invoice-payload)
                         (let [raw (second (re-find #"^test_(.+)$" invoice-payload))]
                           (when raw
                             (try
                               (Long/parseLong raw)
                               (catch Exception _ raw)))))
            db_res (dm/db_query_sender "" dm/user_cart_set_complete_sql {:telegram_user_id (-> message :from :id)
                                                                        :payment_id payment-id})
            order_id (-> db_res first :cart_set_complete :order_id)
            buyer-chat-id (get-in message [:chat :id])]
        (println "Успешная оплата:" db_res)
        (println successful-payment)
        (when payment-id
          (dm/db_query_sender "" dm/exchange_history_upd_sql
                              {:history_id (str payment-id)
                               :service_type "youkassa"
                               :response {:status "paid"
                                          :order_id order_id
                                          :successful_payment successful-payment}})) 
        
        (when order_id
          (notify-order-created order_id buyer-chat-id)
          )
        (when order_id
          (let [order-res (dm/db_query_sender "" dm/order_user_order_get_sql {:order_id order_id})
                order-data (or (-> order-res first :data)
                               (-> order-res first :_r)
                               (-> order-res first :order_user_order_get))
                delivery-provider-name (or (:delivery_provider_name order-data)
                                           (get order-data "delivery_provider_name"))
                delivery-attrs (or (:delivery_attributes order-data)
                                   (get order-data "delivery_attributes"))]
            (when (= delivery-provider-name "cdek")
              (let [cdek-request (or (:cdek_order_draft delivery-attrs)
                                     (get delivery-attrs "cdek_order_draft"))
                    cdek-request (if (map? cdek-request)
                                   (update cdek-request :packages ensure-packages)
                                   cdek-request)]
                (println "[CDEK] order_create start (successful_payment)")
                (println "[CDEK] order_id:" order_id)
                (println "[CDEK] payment_id:" payment-id)
                (println "[CDEK] delivery_attributes present:" (boolean delivery-attrs))
                (println "[CDEK] cdek_request:" (pr-str cdek-request))
                (when (seq cdek-request)
                  (let [resp (safe-cdek-order-create {:telegram_user_id (-> message :from :id)
                                                      :history_id (str payment-id)
                                                      :order_id order_id
                                                      :payment_id payment-id
                                                      :delivery_provider_name "cdek"
                                                      :cdek_request cdek-request}
                                                     :successful_payment)]
                    (fetch-track-number! order_id resp (str payment-id) (-> message :from :id))))))))
        )
      )
    

    {:status 200 :body "OK"}
    )
    )


(defn payment_provider_get [req]
  (println "payment_provider_get")
  (let [params (:params req)
        db_res (dm/db_query_sender "" dm/order_payment_provider_get_sql {})]
    (println db_res)
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}
    )
  )

(defn exchange_history_get [req]
  (let [params (:params req)
        history_id (:history_id params)
        db_res (dm/db_query_sender "" dm/exchange_history_get_sql {:history_id history_id})]
    {:status 200
     :headers {"Content-Type" "text/json"}
     :body (json/write-str db_res)}))


(defn order_get [req]
  (let [req_body (:params req)
        order_id (:order_id req_body)
        db_res (dm/db_query_sender "" dm/order_user_order_get_sql {:order_id order_id})
        ]
    (println "order_get")
    (println db_res)
    {:status  200
     :headers {"Content-Type" "text/json"}
     :body    (json/write-str db_res)}))





(defroutes api-routes 
  ;каталог
  (POST  "/filters-get"                []  filters_get)
  (POST  "/banner-get"                 []  banner_get)
  ;товар
  (POST  "/unit-get"                   []  unit_get)
  (POST  "/product-get-config"         []  product_get_config)
  ;история заказов
  (POST  "/orders-history-get"         []  orders_history_get)
  (POST  "/order-get"                  []  order_get)
  ;создание заказа
  (POST  "/payment-add"                []  payment_add)
  ;cdek
  (POST  "/cdek-auth"                   []  cdek/cdek-auth)
  (POST  "/cdek-calculate"              []  cdek/cdek-calculate)
  (POST  "/cdek-order-create"           []  cdek/cdek-order-create)
  (POST  "/cdek-delivery-points"        []  cdek/cdek-delivery-points)
  (POST  "/cdek-search-pvz"             []  cdek/cdek-delivery-points)
  (POST  "/cdek-search-city"            []  cdek/cdek-search-city)
  ;общие
  (POST  "/payment-provider-get"       []  payment_provider_get)
  (POST  "/exchange-history-get"       []  exchange_history_get)
  (POST  "/delivery-provider-get"      []  delivery_provider_get)
  (POST  "/products-get"               []  products_get)
  (POST  "/product-get"                []  product_get)
  (POST  "/cart-get"                   []  cart_get)
  (POST  "/cart-set"                   []  cart_set)
  (POST  "/cart-get-summary"           []  cart_get_summary)
  (POST  "/favorite-get"               []  favorite_get)
  (POST  "/favorite-add"               []  favorite_add)
  (POST  "/favorite-del"               []  favorite_del)
  (POST  "/user-add"                   []  user_add)
  (POST  "/policies-get"               []  policies_get)
  (POST  "/policies-get-all"           []  policies_get_all)
  (POST  "/user-get-init"              []  user_get_init)
  (POST  "/catalog-config-get"         []  catalog_config_get)
  (POST  "/settings-get"               []  settings_get)
  (POST  "/store-get"                  []  store_get)
  )

(defroutes webhook-routes
  (POST  "/payment"                    []  telegram-webhook)
  (POST  "/telegram"                   []  telegram-webhook)
  )

(defroutes app-routes
  (context "/api" []
    (-> api-routes
        (auth/wrap-telegram-auth (:bot-token api_keys))))

  (context "/webhook" [] 
    webhook-routes 
    )

  (route/not-found "There is no route you are looking for"))






(def app (-> app-routes
             (wrap-cors :access-control-allow-origin [#".*"]
                        :access-control-allow-methods [:get :post :put :delete :options]
                        :access-control-allow-headers ["Content-Type" "X-Telegram-InitData"])
             wrap-keyword-params
             wrap-params
             wrap-json-params 
             )
  )

(defn -main [& args]
  (server/run-server app {:port (ad/app_data :port)
                          :max-body 1000000000
                          :max-ws 1000000000
                          :max-line 1000000000
                          :timeout 3600000})
  (println (str "Server started on port " (ad/app_data :port))))
