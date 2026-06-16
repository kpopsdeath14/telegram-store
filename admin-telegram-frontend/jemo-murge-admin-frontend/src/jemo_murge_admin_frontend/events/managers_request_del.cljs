(ns jemo-murge-admin-frontend.events.managers-request-del
  (:require [ajax.core :as ajax]
            [jemo-murge-admin-frontend.http-client :as http]
            [jemo-murge-admin-frontend.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [jemo-murge-admin-frontend.api-uri-maker :refer [api_uri_maker]]
            [jemo-murge-admin-frontend.events.managers-request-get :refer [managers_request_get]]
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
