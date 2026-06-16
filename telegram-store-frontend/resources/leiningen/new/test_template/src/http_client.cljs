(ns {{name}}.http-client
  (:require 
   [ajax.core :as ajax] 
   [{{name}}.db :refer [app-state]] 
   )
  )

(defn- get-telegram-headers []
  (when (and (exists? js/Telegram) 
             (.-WebApp js/Telegram)
             (.-initData js/Telegram.WebApp))
    {"X-Telegram-InitData" (.-initData js/Telegram.WebApp)}
    )
  )



(defn- get-telegram-user-id []
  (when (and (exists? js/Telegram)
             (.-WebApp js/Telegram)
             (.-initDataUnsafe js/Telegram.WebApp)
             (.-user js/Telegram.WebApp.initDataUnsafe))
    {:telegram_user_id (.-id js/Telegram.WebApp.initDataUnsafe.user)}))

(defn ajax-request-with-headers [request]
  (let [telegram-headers (get-telegram-headers)
        telegram-user-id (get-telegram-user-id)
        
        merged-headers (if telegram-headers 
                        (merge (:headers request {}) telegram-headers)
                        (:headers request))
        
        merged-params (if telegram-user-id
                       (merge (:params request {}) telegram-user-id)
                       (:params request {}))]
    
    (ajax/ajax-request 
     (-> request
         (assoc :headers merged-headers)
         (assoc :params merged-params))
     )
    )
  )