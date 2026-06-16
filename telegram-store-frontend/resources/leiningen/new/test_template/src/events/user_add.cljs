(ns {{name}}.events.user-add
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [clojure.string :as str]
            [{{name}}.apiurimaker :refer [api_uri_maker]]))


(defn user_add_handler [[ok? response]]
  )


(defn user_add [& [start-param]]
  (let [init-data js/Telegram.WebApp.initData
        id (.. js/Telegram -WebApp -initDataUnsafe -user -id)
        params (if (and start-param (not (str/blank? start-param)))
                 {:telegram_user_id id
                  :users_attributes [{:attribute_name "startbot"
                                      :attribute_value start-param
                                      :update_existing false}]}
                 {:telegram_user_id id})]
    (println "[MiniApp] Start param:" start-param)
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "user-add")
      :method :post
      :params params
      :handler user_add_handler
      :headers {"X-Telegram-InitData" init-data}
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))