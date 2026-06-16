(ns {{name}}.events.managers-set
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [{{name}}.api-uri-maker :refer [api_uri_maker]]
            [{{name}}.events.managers-request-get :refer [managers_request_get]]
            [{{name}}.events.managers-get :refer [managers_get]]
            )
  )


(defn managers_set_handler [[ok? response]]
  (managers_get)
  (managers_request_get)
  )


(defn managers_set [managers_data]
  (let []
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "managers-set")
      :method :post
      :params {:managers_data managers_data}
      :handler managers_set_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))