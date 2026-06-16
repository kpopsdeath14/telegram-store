(ns {{name}}.events.store-get
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [{{name}}.api-uri-maker :refer [api_uri_maker]]))


(defn store_get_handler [[ok? response]]
  (let [store (-> response first :_r)]
    (swap! app-state assoc :store store)))


(defn store_get []
  (http/ajax-request-with-headers
   {:uri (api_uri_maker "store-get")
    :method :post
    :params {}
    :handler store_get_handler
    :format (ajax/json-request-format)
    :response-format (ajax/json-response-format {:keywords? true})}))
