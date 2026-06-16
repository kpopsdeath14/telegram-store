(ns {{name}}.services.youkassa
  (:require
   [clj-http.client :as http]
   [clojure.data.json :as json]
   [clojure.string :as s]
   [{{name}}.datamodule :as dm]
   [{{name}}.app-data :as ad]))

(def api-keys (ad/app_data :api_keys))

(defn create-telegram-invoice-link
  [payment-id cart provider-token delivery-cost telegram-user-id delivery-provider-name]
  (println "create-telegram-invoice-link")
  (println cart)
  (let [delivery-cost-num (cond
                            (number? delivery-cost) delivery-cost
                            (string? delivery-cost) (try
                                                      (Double/parseDouble (s/replace delivery-cost #"," "."))
                                                      (catch Exception _ 0))
                            :else 0)
        delivery-cost-cents (int (* delivery-cost-num 100))
        total-amount-cents (int (+ (* (reduce + (map :discount_final_summ cart)) 100)
                                   delivery-cost-cents))
        prices (map (fn [product]
                      {:label (str (:product_name (:product_attributes product))
                                   " × " (:quantity product) " шт.")
                       :amount (* (:discount_final_summ product) 100)})
                    cart)
        prices (if (pos? delivery-cost-cents)
                 (conj (vec prices) {:label "Доставка" :amount delivery-cost-cents})
                 (vec prices))]

    (println "Общая сумма (в копейках):" total-amount-cents)
    (println "Массив цен:" prices)

    (let [payload-test {:title "Тестовая оплата заказа"
                        :description (str "Тест: " (count cart) " товаров")
                        :payload (str "test_" payment-id)
                        :provider_token provider-token
                        :currency "RUB"
                        :prices (vec prices)
                        :need_name false
                        :need_email false
                        :need_phone_number false}
          _ (dm/db_query_sender "" dm/exchange_history_add_sql
                                {:service_type "youkassa"
                                 :request payload-test
                                 :context {:telegram_user_id telegram-user-id
                                           :payment_id payment-id
                                           :delivery_cost delivery-cost
                                           :delivery_provider_name delivery-provider-name
                                           :url (str "https://api.telegram.org/bot"
                                                     (:bot-token api-keys)
                                                     "/createInvoiceLink")}
                                 :history_id (str payment-id)})
          response (http/post (str "https://api.telegram.org/bot"
                                   (:bot-token api-keys)
                                   "/createInvoiceLink")
                              {:content-type :json
                               :throw-exceptions false
                               :body (json/write-str payload-test)})
          body (:body response)
          result (try
                   (json/read-str body :key-fn keyword)
                   (catch Exception _ {:raw body}))]

      (println "Ответ Telegram:" body)
      (println "Парсинг результата:" result)
      (when (:ok result)
        (:result result)))))
