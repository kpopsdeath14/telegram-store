(ns {{name}}.events.payment-add
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [{{name}}.apiurimaker :refer [api_uri_maker]]
            [{{name}}.events.order-get :refer [order_get]]
            [{{name}}.events.cart-get :refer [cart_get]]
            [{{name}}.events.cart-get-summary :refer [cart_get_summary]]
            [{{name}}.events.products-get :refer [products_get]]
            [clojure.string :as string]
            )
  )



(defn extract-history-response [response]
  (let [row (first (if (sequential? response) response []))
        data (or (:data row) (:_r row) (:exchange_history_get row) row)]
    (or data {})))

(defn payment-paid? [history]
  (let [resp (or (:response history) (get history "response") history)]
    (or (= "paid" (:status resp))
        (= "paid" (get resp "status"))
        (:order_id resp)
        (get resp "order_id")
        (:successful_payment resp)
        (get resp "successful_payment")
        (= true (:paid resp))
        (= true (get resp "paid")))))

(def cdek-tariff-info
  {136 {:from-type :pvz :to-type :pvz}
   137 {:from-type :pvz :to-type :door}
   138 {:from-type :door :to-type :pvz}
   139 {:from-type :door :to-type :door}})

(defn parse-int-safe [value]
  (try
    (cond
      (number? value) (int value)
      (string? value) (let [parsed (js/parseInt value 10)]
                        (when-not (js/isNaN parsed) parsed))
      :else nil)
    (catch :default _ nil)))

(defn connection-attr [attrs key]
  (let [string-key (name key)]
    (or (get attrs key)
        (get attrs string-key))))

(defn cdek-provider-attrs [state]
  (let [methods (:delivery_methods state)]
    (some (fn [method]
            (when (= "cdek" (:delivery_provider_name method))
              (or (:connection_attributes method) {})))
          (if (sequential? methods) methods []))))

(defn build-cdek-order-draft [state]
  (let [shipping (:shipping_data state)
        attrs (or (cdek-provider-attrs state) {})
        tariff-code (or (parse-int-safe (:cdek_tariff_code shipping))
                        (parse-int-safe (:tariff_code shipping)))
        {:keys [from-type to-type]} (get cdek-tariff-info tariff-code {})
        from-city-code (connection-attr attrs :from_city_code)
        from-pvz-code (connection-attr attrs :from_pvz_code)
        from-address (connection-attr attrs :from_address)
        to-city-code (or (:cdek_city_code shipping) (get shipping "cdek_city_code"))
        to-pvz-code (or (:cdek_pvz_code shipping) (get shipping "cdek_pvz_code"))
        to-address (or (:address shipping) (get shipping "address"))
        full-name (->> [(or (:surname shipping) (get shipping "surname"))
                        (or (:first_name shipping) (get shipping "first_name"))
                        (or (:patronymic shipping) (get shipping "patronymic"))]
                       (remove nil?)
                       (remove #(= "" %))
                       (string/join " "))
        phone (or (:phone shipping) (get shipping "phone"))
        email (or (:mail shipping) (get shipping "mail"))
        comment (or (:comment shipping) (get shipping "comment"))
        draft (cond-> {:tariff_code tariff-code
                       :recipient (cond-> {}
                                    (and (string? full-name) (not (string/blank? full-name)))
                                    (assoc :name full-name)
                                    (and (string? phone) (not (string/blank? phone)))
                                    (assoc :phones [{:number phone}])
                                    (and (string? email) (not (string/blank? email)))
                                    (assoc :email email))}
                (and (string? comment) (not (string/blank? comment)))
                (assoc :comment comment))
        draft (cond-> draft
                (and (= from-type :pvz) (string? from-pvz-code) (not (string/blank? from-pvz-code)))
                (assoc :shipment_point from-pvz-code)
                (and (= from-type :door)
                     (or (and (string? from-city-code) (not (string/blank? from-city-code)))
                         (and (string? from-address) (not (string/blank? from-address)))))
                (assoc :from_location (cond-> {}
                                        (and (string? from-city-code) (not (string/blank? from-city-code)))
                                        (assoc :code from-city-code)
                                        (and (string? from-address) (not (string/blank? from-address)))
                                        (assoc :address from-address)))
                (and (= to-type :pvz) (string? to-pvz-code) (not (string/blank? to-pvz-code)))
                (assoc :delivery_point to-pvz-code)
                (and (= to-type :door)
                     (or (and (string? to-city-code) (not (string/blank? to-city-code)))
                         (and (string? to-address) (not (string/blank? to-address)))))
                (assoc :to_location (cond-> {}
                                      (and (string? to-city-code) (not (string/blank? to-city-code)))
                                      (assoc :code to-city-code)
                                      (and (string? to-address) (not (string/blank? to-address)))
                                      (assoc :address to-address))))]
    (when tariff-code
      draft)))

(defn exchange_history_get [history_id on-success on-error]
  (http/ajax-request-with-headers
   {:uri (api_uri_maker "exchange-history-get")
    :method :post
    :params {:history_id history_id}
    :handler (fn [[ok? response]]
               (if ok?
                 (on-success response)
                 (when on-error (on-error response))))
    :format (ajax/json-request-format)
    :response-format (ajax/json-response-format {:keywords? true})}))

(defn navigate-to-thank-you! [order-id]
  (when order-id
    (swap! app-state assoc
           :last_paid_order_id order-id
           :current_order_id order-id
           :page :thank-you)
    (set! (.-href (.-location js/window)) (str "#/thank-you/" order-id))))

(defn start-payment-poll [payment_id]
  (let [attempts (atom 0)
        max-attempts 120
        delay-ms 3000]
    (letfn [(tick []
              (when (< @attempts max-attempts)
                (swap! attempts inc)
                (exchange_history_get
                 payment_id
                 (fn [response]
                   (let [history (extract-history-response response)
                         paid? (payment-paid? history)
                         order-id (or (:order_id history)
                                      (get history "order_id")
                                      (get-in history [:response :order_id])
                                      (get-in history ["response" "order_id"]))]
                     (if paid?
                       (if order-id
                         (do
                           (cart_get)
                           (cart_get_summary)
                           (order_get order-id)
                           (products_get (:search_value @app-state) (:filters_picked @app-state) (:selected_sorting @app-state))
                           (navigate-to-thank-you! order-id) 
                           )
                         (js/setTimeout tick delay-ms))
                       (js/setTimeout tick delay-ms))
                     )
                   )
                 (fn [_]
                   (js/setTimeout tick delay-ms)))))]
      (tick))))

(defn payment_add_handler [[ok? response]]
  (.hideProgress js/Telegram.WebApp.MainButton)
  (let [payment-id (response :payment_id)
        order-id (response :order_id)
        payment-link (response :payment_link)
        web-app (.-WebApp js/Telegram)]
    (swap! app-state assoc :current_payment_id payment-id)
    (cond
      order-id (do
                 (cart_get)
                 (products_get (:search_value @app-state) (:filters_picked @app-state) (:selected_sorting @app-state))
                 (cart_get_summary)
                 (order_get order-id)
                 (navigate-to-thank-you! order-id))
      payment-link (do
                     (start-payment-poll payment-id)
                     (.openInvoice web-app payment-link
                                   (fn [event]
                                     ;(handle_payment event response)
                                     )
                                   )
                     )
      :else (js/alert "Не удалось создать ссылку на оплату"))))


(defn payment_add []
  (let [shipping (:shipping_data @app-state)
        provider (get shipping :delivery_provider_name)
        cdek-draft (when (= provider "cdek")
                     (build-cdek-order-draft @app-state))
        delivery-attributes (cond-> (or shipping {})
                              cdek-draft (assoc :cdek_order_draft cdek-draft))]
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "payment-add")
      :method :post
      :params {:payment_type (get-in @app-state [:shipping_data :payment_type])
               :delivery_cost (get-in @app-state [:shipping_data :delivery_cost])
               :delivery_provider_name (get-in @app-state [:shipping_data :delivery_provider_name])
               :history_fields shipping
               :delivery_attributes delivery-attributes
               }
      :handler payment_add_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))
