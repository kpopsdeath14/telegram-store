(ns jemo-murge-admin-frontend.events.banner-del
  (:require [ajax.core :as ajax]
            [jemo-murge-admin-frontend.http-client :as http]
            [jemo-murge-admin-frontend.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [jemo-murge-admin-frontend.api-uri-maker :refer [api_uri_maker]]
            )
  )





(defn banner_image_del_handler [[ok? response]]
  
  )




(defn banner_image_del [image_name]
  (let []
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "banner-picture-del")
      :method :post
      :params {:image_name image_name}
      :handler  banner_image_del_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))
