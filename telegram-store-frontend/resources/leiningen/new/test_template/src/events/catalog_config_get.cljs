(ns {{name}}.events.catalog-config-get
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [{{name}}.apiurimaker :refer [api_uri_maker]]))


(defn catalog_config_get_handler [[ok? response]]
  (swap! app-state
         (fn [state]
           (let [next-state (assoc state
                                   :catalog_config (-> response first :_r)
                                   :catalog_config_loaded? true)]
             (assoc next-state
                    :app_ready?
                    (and (:settings_loaded? next-state)
                         (:catalog_config_loaded? next-state)))))))


(defn catalog_config_get []
  (let []
   (http/ajax-request-with-headers
    {:uri (api_uri_maker "catalog-config-get")
     :method :post
     :params {}
     :handler catalog_config_get_handler
     :format (ajax/json-request-format)
     :response-format (ajax/json-response-format {:keywords? true})})))
