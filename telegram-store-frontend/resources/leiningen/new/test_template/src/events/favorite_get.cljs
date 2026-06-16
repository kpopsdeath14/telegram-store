(ns {{name}}.events.favorite-get
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [{{name}}.apiurimaker :refer [api_uri_maker]]))

(defn favorite_get_handler [[ok? response]]
  (swap! app-state assoc
         :favorites (if ok?
                      (vec (map (fn [item] (:data item)) response))
                      [])
         :favorites_loaded? true))

(defn favorite_get []
  (swap! app-state assoc :favorites_loaded? false)
  (http/ajax-request-with-headers
   {:uri (api_uri_maker "favorite-get")
    :method :post
    :params {}
    :handler favorite_get_handler
    :format (ajax/json-request-format)
    :response-format (ajax/json-response-format {:keywords? true})}))
