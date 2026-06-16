(ns {{name}}.events.cart-to-checkout
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [{{name}}.apiurimaker :refer [api_uri_maker]]
            )
  )



(defn cart_to_checkout_handler [[ok? response]]
  (let [cart (vec (map (fn [product] (:data product)) response))
        parse-int (fn [value]
                    (cond
                      (number? value) value
                      (string? value) (let [parsed (js/parseInt value 10)]
                                        (when-not (js/isNaN parsed) parsed))
                      :else nil))
        issue-ids (->> cart
                       (filter (fn [product]
                                 (let [stock (parse-int (:stock_quantity product))
                                       qty (parse-int (:quantity product))]
                                   (and (number? stock)
                                        (number? qty)
                                        (> qty stock)))))
                       (map :product_id)
                       set)]
    (swap! app-state assoc :cart cart
                           :cart_stock_issues {:show? (not (empty? issue-ids))
                                               :items issue-ids})
    (.hideProgress js/Telegram.WebApp.MainButton)
    (when (empty? issue-ids)
      (swap! app-state assoc :page :delivery)
      (set! (.-href (.-location js/window)) "#/delivery")
      (js/window.scrollTo 0 0))))


(defn cart_to_checkout []
  "функция вызываемая при переходе от корзины до оформления заказа. ее задача - проверить количество товаров
     (и, возможно, остатки на складе). в зависимости от сервиса которым пользуется клиент будут вызываться различные методы
     обращения к этим сервисам. общая идея такая:
     1) перезапрос корзины - чтобы была актуальная информация сколько в корзине и сколько на складе товаров по
     каждому товару из корзины пользователя
     2) проверка количества каждого товара
     3) если все ок - переход на страницу оформления заказа"
  (let []
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "cart-get")
      :method :post
      :params {}
      :handler cart_to_checkout_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))
