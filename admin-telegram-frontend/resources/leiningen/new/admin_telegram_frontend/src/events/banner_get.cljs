(ns {{name}}.events.banner-get
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [{{name}}.api-uri-maker :refer [api_uri_maker]]
            )
  )



(defn banner_get_handler [[ok? response]]
  (swap! app-state assoc :banners (or (some #(when (= (:banner_location %) "main_page") %) (if ok?
                                                                                             (mapv (fn [product] (:data product)) response)
                                                                                             [])
                                            ) 
                                      {})
         )
  )


(defn banner_get [filters]
  (let []
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "banner-get")
      :method :post
      :params filters
      :handler banner_get_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})
      }
     )
    )
  )