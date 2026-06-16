(ns {{name}}.events.favorite-add
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.events.favorite-get :refer [favorite_get]]
            [{{name}}.apiurimaker :refer [api_uri_maker]]))

(defn favorite_add_handler [[ok? _response]]
  (favorite_get))

(defn favorite_add [product_id telegram_user_id]
  (let [entry (cond-> {:product_id product_id}
                telegram_user_id (assoc :telegram_user_id telegram_user_id))]
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "favorite-add")
      :method :post
      :params {:favorites [entry]}
      :handler favorite_add_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))
