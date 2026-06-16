(ns {{name}}.events.banners-get
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [{{name}}.apiurimaker :refer [api_uri_maker]])
  )


(defn banner_get_handler [[ok? response]]
  (swap! app-state assoc
         :banners (vec (map (fn [product] (:data product)) response))
         :banners_loaded? true)
  )


(defn banner_get [filters]
  (let []
    (when-not (:banners_loaded? @app-state)
      (swap! app-state assoc :banners_loaded? false))
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "banner-get")
      :method :post
      :params filters
      :handler banner_get_handler
      :headers {}
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))
