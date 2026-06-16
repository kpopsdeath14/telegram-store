(ns jemo-murge-admin-frontend.pages.login.login
  (:require 
   [reagent.core :as reagent]
   [jemo-murge-admin-frontend.db :refer [app-state]]
   [jemo-murge-admin-frontend.local-storage :as ls]
   [jemo-murge-admin-frontend.events.user-get-init :refer [user_get_init]]
   )
  )

(defn load-telegram-script []
  (let [script (.createElement js/document "script")]
    (set! (.-src script) "https://telegram.org/js/telegram-widget.js?22")
    (set! (.-async script) true)
    (.appendChild (.-body js/document) script)))

(defn telegram-login-button [{:keys [bot-name on-auth size request-access]}]
  (reagent/create-class
   {:component-did-mount
    (fn [this]
      (load-telegram-script)
      (let [props (reagent/props this)
            {:keys [on-auth bot-name size request-access]} props
            div (.getElementById js/document "telegram-login")]

        (set! (.-onTelegramAuth js/window) on-auth)

        (set! (.-innerHTML div)
              (str "<script async src=\"https://telegram.org/js/telegram-widget.js?22\" 
                   data-telegram-login=\"" (or bot-name "your_default_bot") "\" 
                   data-size=\"" (or size "large") "\" 
                   data-onauth=\"onTelegramAuth(user)\"
                   data-request-access=\"" (or request-access "write") "\"></script>")))

      (js/console.log "onTelegramAuth установлена?" (fn? (.-onTelegramAuth js/window))))

    :component-will-unmount
    (fn [this]
      (set! (.-onTelegramAuth js/window) nil))

    :reagent-render
    (fn []
      [:div {:style {:height "100vh"}}
       [:div {:id "telegram-login"
              :style {:display "flex"
                      :justify-content "center"
                      :align-items "center"
                      :height "100%"}}]])}))

(defn login_page []
  [telegram-login-button
   {:bot-name (:admin_bot_name (:config @app-state))
    :size "large"
    :request-access "write"
    :on-auth (fn [user]
               
               (let [user-obj (js->clj user :keywordize-keys true)] 
                 (let [user-data-hash (->> user-obj
                                           (sort-by key)
                                           (map (fn [[k v]] (str (name k) "=" v)))
                                           (clojure.string/join "\n")
                                           (js/encodeURIComponent))]
                   (ls/set-item! "browser-hash-data" user-data-hash)
                   (ls/set-item! "telegram_user_id" (:id user-obj))
                   (js/location.reload) 
                   )
                 )
               )
    }
   ]
   )
