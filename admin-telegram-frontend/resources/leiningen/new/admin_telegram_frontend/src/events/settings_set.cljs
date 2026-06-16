(ns {{name}}.events.settings-set
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [{{name}}.api-uri-maker :refer [api_uri_maker]]
            [{{name}}.events.settings-get :refer [settings_get]]
            )
  )


(defn settings_set_handler [[ok? response]] 
  (settings_get)
  )



(defn settings_set [config_name config_value]
  (swap! app-state assoc (keyword (str "settings_" config_name)) nil)
  (let []
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "settings-set")
      :method :post
      :params {:config_name config_name :config_value config_value}
      :handler settings_set_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))