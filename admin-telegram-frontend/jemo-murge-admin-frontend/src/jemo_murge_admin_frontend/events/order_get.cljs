(ns jemo-murge-admin-frontend.events.order-get
  (:require [ajax.core :as ajax]
            [jemo-murge-admin-frontend.http-client :as http]
            [jemo-murge-admin-frontend.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [jemo-murge-admin-frontend.api-uri-maker :refer [api_uri_maker]]
            )
  )


(defn order_get_handler [[ok? response]]
  (swap! app-state assoc :order (first (vec (map (fn [product] (:data product)) response))))
  )


(defn order_get [order_id]
  (let []
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "order-get")
      :method :post
      :params {:order_id order_id}
      :handler order_get_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))
