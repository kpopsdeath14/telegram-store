(ns {{name}}.events.user-get-init
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [{{name}}.apiurimaker :refer [api_uri_maker]]))


(defn user_get_init_handler [[ok? response]]
  (let [main_button js/Telegram.WebApp.MainButton
        info (:user_get_init (first response))
        user-status (:user_status info)
        state (:app_state info)
        technical-work? (= "technical_work" state)
        ] 
    (swap! app-state assoc
           :user_status user-status
           :app_state state
           :technical_work? technical-work?) 
    )
  )


(defn user_get_init []
  (let []
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "user-get-init")
      :method :post
      :params {}
      :handler user_get_init_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))
