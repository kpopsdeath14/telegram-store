(ns jemo-murge-admin-frontend.events.filters-get
  (:require [ajax.core :as ajax]
            [jemo-murge-admin-frontend.http-client :as http]
            [jemo-murge-admin-frontend.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [jemo-murge-admin-frontend.api-uri-maker :refer [api_uri_maker]]))


(defn filters_get_handler [[ok? response]]
  (swap! app-state assoc :filters
         (if ok?
           (mapv (fn [product] (:data product)) response)
           []
           )
         )
  )


(defn filters_get [filters]
  (let []
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "filters-get")
      :method :post
      :params {:filters filters}
      :handler filters_get_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))
