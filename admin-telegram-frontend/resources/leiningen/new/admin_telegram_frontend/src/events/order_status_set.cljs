(ns {{name}}.events.order-status-set
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [{{name}}.api-uri-maker :refer [api_uri_maker]]
            [{{name}}.events.order-get :refer [order_get]]
            )
  )


(defn order_status_set_handler [[ok? response]]
  (order_get (:current_order_id @app-state))
  )


(defn order_status_set [order_id status_name]
  (let []
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "order-status-set")
      :method :post
      :params {:order_id order_id :status_name status_name}
      :handler order_status_set_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))
