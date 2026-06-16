(ns jemo-murge-admin-frontend.events.banner-add
  (:require [ajax.core :as ajax]
            [jemo-murge-admin-frontend.http-client :as http]
            [jemo-murge-admin-frontend.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [jemo-murge-admin-frontend.api-uri-maker :refer [api_uri_maker]]
            [jemo-murge-admin-frontend.events.banner-get :refer [banner_get]]
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
