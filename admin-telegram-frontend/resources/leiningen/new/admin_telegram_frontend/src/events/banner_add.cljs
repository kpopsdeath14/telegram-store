(ns {{name}}.events.banner-add
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [{{name}}.api-uri-maker :refer [api_uri_maker]]
            [{{name}}.events.banner-get :refer [banner_get]]
            )
  )



(defn banner_add_handler [[ok? response]]
  (banner_get {:banner_location "main_page"})
  )


(defn banner_add [filters]
  (let []
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "banner-add")
      :method :post
      :params filters
      :handler banner_add_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})
      }
     )
    )
  )