(ns {{name}}.events.favorite-del
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.events.favorite-get :refer [favorite_get]]
            [{{name}}.apiurimaker :refer [api_uri_maker]]))

(defn favorite_del_handler [[ok? _response]]
  (favorite_get))

(defn favorite_del [product_id telegram_user_id]
  (let [entry (cond-> {:product_id product_id}
                telegram_user_id (assoc :telegram_user_id telegram_user_id))]
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "favorite-del")
      :method :post
      :params {:favorites [entry]}
      :handler favorite_del_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))
