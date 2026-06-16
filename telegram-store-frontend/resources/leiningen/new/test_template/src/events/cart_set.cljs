(ns {{name}}.events.cart-set
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [{{name}}.apiurimaker :refer [api_uri_maker]]
            [{{name}}.events.cart-get :refer [cart_get]]
            [{{name}}.events.cart-get-summary :refer [cart_get_summary]]
            )
  )


(defn cart_set_handler [product-id]
  (fn [[ok? response]]
    (if (and ok? (map? response) (or (:cart_get response) (:cart_get_summary response)))
      (let [cart-items (:cart_get response)
            summary-items (:cart_get_summary response)]
        (when cart-items
          (swap! app-state assoc
                 :cart (vec (map (fn [product] (:data product)) cart-items))
                 :cart_loaded? true))
        (when summary-items
          (swap! app-state assoc
                 :cart_summary (-> summary-items first :cart_get_summary)
                 :cart_summary_loaded? true)))
      (do
        (cart_get)
        (cart_get_summary)))
    (swap! app-state update :cart_pending_ids (fnil disj #{}) product-id)))



(defn cart_set [product_id quantity]
  (let []
    (when (nil? product_id)
      (throw (js/Error. "cart_set: product_id is nil")))
    (let [normalized (cond
                       (number? quantity) (js/Math.floor quantity)
                       (string? quantity) (let [parsed (js/parseInt quantity 10)]
                                            (if (js/isNaN parsed) 0 parsed))
                       :else 0)
          safe-qty (max 0 normalized)]
    (swap! app-state update :cart_pending_ids (fnil conj #{}) product_id)
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "cart-set")
      :method :post
      :params {:product_id product_id :quantity safe-qty}
      :handler (cart_set_handler product_id)
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})}))))
