(ns jemo-murge-admin-frontend.events.catalog-bot-description-set
  (:require [ajax.core :as ajax]
            [jemo-murge-admin-frontend.http-client :as http]
            [jemo-murge-admin-frontend.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [jemo-murge-admin-frontend.api-uri-maker :refer [api_uri_maker]]
            [jemo-murge-admin-frontend.events.settings-get :refer [settings_get]]
            )
  )


(defn catalog_bot_description_set_handler [[ok? response]]
  (settings_get)
  )


(defn catalog_bot_description_set
  ([description]
   (catalog_bot_description_set description nil))
  ([description image-url]
   (http/ajax-request-with-headers
    {:uri (api_uri_maker "catalog-bot-description-set")
     :method :post
     :params {:description description
              :image_url image-url}
     :handler catalog_bot_description_set_handler
     :format (ajax/json-request-format)
     :response-format (ajax/json-response-format {:keywords? true})})))
