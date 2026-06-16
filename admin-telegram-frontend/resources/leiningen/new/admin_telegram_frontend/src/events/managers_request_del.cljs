(ns {{name}}.events.managers-request-del
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [{{name}}.api-uri-maker :refer [api_uri_maker]]
            [{{name}}.events.managers-request-get :refer [managers_request_get]]
            )
  )


(defn managers_request_del_handler [[ok? response]]
  (managers_request_get)
  )


(defn managers_request_del [manager_user_id]
  (let []
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "managers-request-del")
      :method :post
      :params {:manager_user_id manager_user_id}
      :handler managers_request_del_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))