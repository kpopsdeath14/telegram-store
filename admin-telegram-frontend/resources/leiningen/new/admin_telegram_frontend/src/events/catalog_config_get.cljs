(ns {{name}}.events.catalog-config-get
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [{{name}}.api-uri-maker :refer [api_uri_maker]]))


(defn catalog_config_get_handler [[ok? response]]
  (swap! app-state assoc :filters
         (if ok?
           (mapv (fn [product] (:data product)) response)
           []
           )
         )
  )


(defn catalog_config_get [filters]
  (let []
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "catalog-config-get")
      :method :post
      :params {:filters filters}
      :handler catalog_config_get_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))