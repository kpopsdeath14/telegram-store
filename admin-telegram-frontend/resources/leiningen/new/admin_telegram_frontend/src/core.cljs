(ns {{name}}.core
  (:require
   ["antd" :as antd]
   [reagent.dom :as d]
   [{{name}}.router :refer [routes]]
   [{{name}}.db :refer [app-state]]
   [{{name}}.viewes :refer [current-page]]
   [{{name}}.pages.header.header :refer [header]]
   [{{name}}.pages.login.login :refer [login_page]]
   [{{name}}.local-storage :as ls]

   [{{name}}.events.units-get :refer [units_get]]
   [{{name}}.events.filters-get :refer [filters_get]]
   [{{name}}.events.model-unit-get :refer [model_unit_get]]
   [{{name}}.events.user-get-init :refer [user_get_init]]
   [{{name}}.events.banner-get :refer [banner_get]]
   [{{name}}.events.orders-history-get :refer [orders_history_get]]
   [{{name}}.events.store-get :refer [store_get]]

   [reagent.core :as reagent]
   [clojure.string :as str]
   [clojure.edn :as edn]
   ))

(defn parse-date [s]
  (when (and s (not (str/blank? s)))
    (let [d (js/Date. s)]
      (when-not (js/isNaN (.getTime d)) d))))

(defn store-tariff-active? [store]
  (let [current-tariff (:current_tariff store)
        ended-at       (:ended_at current-tariff)
        planned-until  (:planned_until current-tariff)
        planned-date   (parse-date planned-until)]
    (cond
      (nil? current-tariff) false
      (some? ended-at)      false
      planned-date          (>= (.getTime planned-date) (.getTime (js/Date.)))
      :else                 true)))

(defn page_template []
  (let [Layout antd/Layout
        Content (.-Content Layout)
        ConfigProvider antd/ConfigProvider
        user (.. js/Telegram -WebApp -initDataUnsafe -user)
        web-app (.-WebApp js/Telegram)

        development (reagent/cursor app-state [:development])
        login? (reagent/cursor app-state [:login?])
        ]
    (fn []
      (let [store (:store @app-state)
            store-inactive? (and (some? store) (not (store-tariff-active? store)))]
        [:> ConfigProvider {:theme {:components {:Button {}
                                                 :Layout {:bodyBg "#ffffff"}
                                                 :Select {}}}
                            :wave {:disabled true}
                            :token {:fontFamily "'AA Ordinar', sans-serif"}}
         [:> Layout {:style {:overflow-y "hidden"}}
          [header]
          (when store-inactive?
            [:div {:style {:position "sticky"
                           :top 0
                           :zIndex 20
                           :background "#fff3cd"
                           :borderBottom "1px solid #ffc107"
                           :color "#111"
                           :padding "8px 16px"
                           :fontSize 12
                           :textAlign "center"
                           :cursor "pointer"}
                   :onClick (fn [] (.openTelegramLink web-app "https://t.me/bibi_zen_bot"))}
             "МАГАЗИН НЕАКТИВЕН. Требуется оплата доступа"])
          [:> Content
           {:style
            {:min-height "100vh"
             :padding "15px 7% 0 7%"}}

           (if @login?
             [login_page]

             (cond
               (= "normis" (:user_status @development))
               (str "чтобы подать заявку на менеджера, напишите /request боту @" (:bot-name (:config @app-state)))

               (and (not (= (:user_status @development) "admin")) (= "technical_work" (:app_state @development)))
               [:div {:style {:display "flex"
                              :flex-direction "column"
                              :justify-content "center"
                              :align-items "center"
                              :height "100vh"
                              :width "100vw"}}
                [:div {:style {:display "flex"
                               :align-items "center"
                               :margin-bottom "20px"}}
                 [:div {:style {:width "20px"
                                :height "20px"
                                :background-color "#1890ff"
                                :border-radius "50%"
                                :margin-right "10px"
                                :animation "pulse 1.5s infinite"}}]
                 [:div {:style {:width "20px"
                                :height "20px"
                                :background-color "#1890ff"
                                :border-radius "50%"
                                :margin-right "10px"
                                :animation "pulse 1.5s infinite 0.5s"}}]
                 [:div {:style {:width "20px"
                                :height "20px"
                                :background-color "#1890ff"
                                :border-radius "50%"
                                :animation "pulse 1.5s infinite 1s"}}]]
                [:h1 {:style {:color "#262626"
                              :font-size "2.5rem"
                              :font-weight 700
                              :text-align "center"}}
                 "ведутся технические работы"]
                [:style
                 "@keyframes pulse {
                    0% { opacity: 1; transform: scale(1); }
                    50% { opacity: 0.5; transform: scale(0.9); }
                    100% { opacity: 1; transform: scale(1); }
                  }"]]

               :else
               [current-page]
               )
             )

           ]
          ]
         ]
        )
      )
    )
    )

(defn mount-root []
  (let [units-filters-from-state (fn [state]
                                   (merge (:filters_picked state)
                                          {:actual (case (:products_mode state)
                                                     "catalog" ["t" "true"]
                                                     "archive" ["f" "false"])}))
        units-search-from-state (fn [state]
                                  (or (:search_value state) ""))
        main_button js/Telegram.WebApp.MainButton]
    (add-watch app-state :page_listener
               (fn [key atom old-state new-state]
                 (if-not (= (:products_mode new-state) (:products_mode old-state))
                   (case (:page new-state)
                     :units (units_get (units-filters-from-state new-state)
                                       (units-search-from-state new-state))
                     :model_unit (model_unit_get (assoc {}
                                                        :unit_id [(:current_unit_id @app-state)]
                                                        :actual (case (:products_mode @app-state)
                                                                  "catalog" ["t" "true"]
                                                                  "archive" ["f" "false"])) "")
                     :nil))

                 (if-not (= (:orders_history_filters_picked new-state) (:orders_history_filters_picked old-state))
                   (orders_history_get (:orders_history_filters_picked new-state)))

                 (when (and (not= (:login? new-state) (:login? old-state))
                            (false? (:login? new-state)))
                   (filters_get []))

                 (when (and (= (:page new-state) :units)
                            (or (not= (:filters_picked new-state) (:filters_picked old-state))
                                (not= (:search_value new-state) (:search_value old-state))))
                   (units_get (units-filters-from-state new-state)
                              (units-search-from-state new-state)))))

    (add-watch app-state :main_button_visibility
               (fn [_ _ _ new-state]
                 (let [user-status (get-in new-state [:development :user_status])
                       app-state (get-in new-state [:development :app_state])]
                   (when (and user-status
                              (not= "admin" user-status)
                              (= "technical_work" app-state))
                     (.hide main_button))))))

  (let [current-hash (.-hash js/window.location)]
    (when (or (empty? current-hash) (= "#" current-hash))
      (set! (.-hash js/window.location) "#/")))
  (routes)
  (d/render [page_template] (.getElementById js/document "app")))


(defn load-config []
  (-> (js/fetch "config.edn")
      (.then (fn [response] (.text response)))
      (.then (fn [text] (edn/read-string text)))
      (.catch (fn [error] (js/console.error "Error loading config:" error)))
      (.then (fn [config]
               (let [web-app (.-WebApp js/Telegram)
                     user (.. js/Telegram -WebApp -initDataUnsafe -user)
                     start_param (.. js/Telegram -WebApp -initDataUnsafe -start_param)
                     back-button (.-BackButton web-app)
                     main_button js/Telegram.WebApp.MainButton
                     browser-hash-data (ls/get-item "browser-hash-data")
                     ]
                 (swap! app-state assoc :config config) 
                 
                 (when browser-hash-data
                   (swap! app-state assoc :user_data_hash browser-hash-data)
                   (user_get_init (ls/get-item "telegram_user_id"))
                   )
                 
                 (if user
                   (user_get_init (.-id user))
                   )
                 
                 (banner_get {:banner_location "main_page"})
                 (store_get)
                 (units_get {:actual ["t" "true"]} "")

                 (filters_get [])
                 
                 (mount-root)
                 ) 
               )
             )
      )
  )


(defn ^:export init! []
  (let [web-app (.-WebApp js/Telegram)
        user (.. js/Telegram -WebApp -initDataUnsafe -user)
        start_param (.. js/Telegram -WebApp -initDataUnsafe -start_param)
        back-button (.-BackButton web-app)
        main_button js/Telegram.WebApp.MainButton]
    
    (load-config) 
    )
  )
