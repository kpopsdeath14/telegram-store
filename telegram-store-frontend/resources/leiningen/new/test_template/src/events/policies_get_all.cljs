(ns {{name}}.events.policies-get-all
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [{{name}}.apiurimaker :refer [api_uri_maker]]
            )
  )




(defn policies_get_all_handler [[ok? response]]
  (swap! app-state assoc :policies_all
         (if ok?
           (mapv (fn [product] (:data product)) response)
           {})
         )
  )


(defn policies_get_all []
  (let []
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "policies-get-all")
      :method :post
      :params {}
      :handler policies_get_all_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))