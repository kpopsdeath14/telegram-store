(ns jemo-murge-admin-frontend.events.second-level-get-config
  (:require [ajax.core :as ajax]
            [jemo-murge-admin-frontend.http-client :as http]
            [jemo-murge-admin-frontend.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [jemo-murge-admin-frontend.api-uri-maker :refer [api_uri_maker]]
            )
  )


(defn second_level_get_config_handler [[ok? response]]
  (swap! app-state assoc :second_lvl_config
         (if ok?
           (:_r (first response))
           {}
           )
         ) 
  )


(defn second_level_get_config []
  (let []
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "second-lvl-get-config")
      :method :post
      :params {}
      :handler second_level_get_config_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))
