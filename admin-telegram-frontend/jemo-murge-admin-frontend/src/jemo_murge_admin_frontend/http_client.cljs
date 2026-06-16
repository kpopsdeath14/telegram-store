(ns jemo-murge-admin-frontend.http-client
  (:require 
   [ajax.core :as ajax] 
   [jemo-murge-admin-frontend.db :refer [app-state]]
   [jemo-murge-admin-frontend.local-storage :as ls]
   )
  )

(defn get-telegram-headers [] 
  (let [init-data (when (and (exists? js/Telegram)
                             (.-WebApp js/Telegram)
                             (.-initData js/Telegram.WebApp))
                    (.-initData js/Telegram.WebApp))]
    {"X-Telegram-InitData" (or init-data "")
     "X-Telegram-Hash" (:user_data_hash @app-state "")})
  )

(defn get-telegram-user-id []
  (when (and (exists? js/Telegram)
             (.-WebApp js/Telegram)
             (.-initDataUnsafe js/Telegram.WebApp)
             (.-user js/Telegram.WebApp.initDataUnsafe))
    (.-id js/Telegram.WebApp.initDataUnsafe.user))
  
  (let [telegram_user_id (when (and (exists? js/Telegram)
                                    (.-WebApp js/Telegram)
                                    (.-initDataUnsafe js/Telegram.WebApp)
                                    (.-user js/Telegram.WebApp.initDataUnsafe))
                           (.-id js/Telegram.WebApp.initDataUnsafe.user))
        ]
    (or telegram_user_id (ls/get-item "telegram_user_id"))
    )
  
  )

(defn ajax-request-with-headers [request]
  (let [telegram-headers (get-telegram-headers)
        telegram-user-id (get-telegram-user-id)
        
        merged-headers (if telegram-headers 
                         (merge (:headers request {}) telegram-headers)
                         (:headers request))
        
        merged-params (if telegram-user-id
                        (merge (:params request {}) {:telegram_user_id telegram-user-id})
                        (:params request))]
    
    (ajax/ajax-request
     (-> request
         (assoc :headers merged-headers)
         (assoc :params merged-params)))))
