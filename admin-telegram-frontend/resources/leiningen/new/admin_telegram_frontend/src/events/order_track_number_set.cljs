(ns {{name}}.events.order-track-number-set
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [{{name}}.api-uri-maker :refer [api_uri_maker]]
            [{{name}}.events.order-get :refer [order_get]]
            )
  )

(defn order_track_number_set_handler [[ok? _response]]
  (order_get (:current_order_id @app-state)))

(defn order_track_number_set [order_id track_number]
  (http/ajax-request-with-headers
   {:uri (api_uri_maker "order-track-number-set")
    :method :post
    :params {:order_id order_id :track_number track_number}
    :handler order_track_number_set_handler
    :format (ajax/json-request-format)
    :response-format (ajax/json-response-format {:keywords? true})}))
