(ns {{name}}.pages.catalog.logo
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [{{name}}.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]]
   [clojure.string :as str]))


(defn is-mobile? []
  (let [user-agent (.-userAgent (.-navigator js/window))
        mobile-regex #"(?i)android|webos|iphone|ipad|ipod|blackberry|iemobile|opera mini"]
    (boolean (re-find mobile-regex user-agent))))


(defn logo []
  (let [Drawer antd/Drawer
        Button antd/Button

        side_menu_open (reagent/cursor app-state [:side_menu_open])
        settings (reagent/cursor app-state [:settings])
        app-ready? (reagent/cursor app-state [:app_ready?])
        ]
    (fn []
      (let [support-nickname (some-> @settings :customer :customer_contact_nickname str)
            cleaned-nickname (some-> support-nickname (str/replace #"^@" ""))
            support-link (when (seq support-nickname)
                           (if (re-matches #"\d+" support-nickname)
                             (str "tg://user?id=" (str "@" support-nickname))
                             (str "https://t.me/" cleaned-nickname)))
            support-label (when (seq support-nickname)
                            (if (re-matches #"\d+" support-nickname)
                              (str "ID @" support-nickname)
                              (str "@" cleaned-nickname))) 
            
            web-app (.-WebApp js/Telegram)
            platform (when web-app (.-platform web-app))
            is-tg-mobile? (contains? #{"ios" "android"} platform)
            is-mobile-device? (is-mobile?)
            is-mobile (or is-tg-mobile? is-mobile-device?)
            padding-top 87
            current-page (:page @app-state)
            nav-items [{:key :catalog
                        :label "Каталог"
                        :icon icons/AppstoreOutlined}
                       {:key :favorites
                        :label "Избранное"
                        :icon icons/HeartOutlined}
                       {:key :information
                        :label "Информация"
                        :icon icons/InfoCircleOutlined}
                       {:key :orders-history
                        :label "История заказов"
                        :icon icons/HistoryOutlined}
                       ]
            ]
        [:div {:style {:width "100%"
                       :min-height (if is-mobile
                                     (+ 64 padding-top)
                                     64)
                       :display "flex"
                       :justify-content "space-between"
                       :align-items "center" 
                       :padding-top (if is-mobile
                                      padding-top
                                      0
                                      )
                       :background "#090909"
                       :padding-left 38
                       :padding-right 27
                       :color "#ffffff"}
               :onClick (fn []
                          (let [web-app (.-WebApp js/Telegram)
                                telegram-url "tg://user?id=286606382"]
                            (.openTelegramLink web-app telegram-url)
                            )
                          )
               }
         [:> Drawer {:title nil
                     :open @side_menu_open
                     :closable false
                     :onClose (fn []
                                (swap! app-state assoc :side_menu_open false)
                                )
                     :bodyStyle {:display "flex"
                                 :flex-direction "column"
                                 :height "100%"
                                 :paddingTop (if is-mobile
                                               (+ 18 padding-top)
                                               18
                                               )
                                 :paddingRight 18
                                 :paddingBottom 16
                                 :paddingLeft 18
                                 
                                 }
                     :style {:borderRadius "16px 0 0 16px"}}
          [:div {:style {:display "flex"
                         :alignItems "center"
                         :justifyContent "space-between"
                         :marginBottom 16}}
           [:div {:style {:fontSize 18
                          :fontWeight 500
                          :color "#111"}}
            "Навигация"]
           [:> Button {:type "text"
                       :shape "circle"
                       :size "middle"
                       :onClick (fn []
                                  (swap! app-state assoc :side_menu_open false))
                       :style {:width 32
                               :height 32
                               :display "flex"
                               :alignItems "center"
                               :justifyContent "center"
                               :padding 0}
                       :icon (as-element [:> icons/CloseOutlined {:style {:fontSize 16
                                                                          :color "#6f6f6f"}}])}]]

          [:div {:style {:display "flex"
                         :flexDirection "column"
                         :gap 10}}
           (for [{:keys [key label icon]} nav-items]
             (let [selected? (= key current-page)
                   Icon icon]
               ^{:key (name key)}
               [:button {:type "button"
                         :onClick (fn []
                                    (swap! app-state assoc :page key)
                                    (set! (.-href (.-location js/window)) (str "#/" (name key)))
                                    (swap! app-state assoc :side_menu_open false)
                                    )
                         :style {:display "flex"
                                 :alignItems "center"
                                 :gap 12
                                 :width "100%"
                                 :height 52
                                 :padding "0 16px"
                                 :borderRadius 12
                                 :border (if selected? "1px solid var(--color-border)" "1px solid #e6e6e6")
                                 :background (if selected? "#f3f3f3" "#ffffff")
                                 :color "#111"
                                 :fontSize 16
                                 :fontWeight 500
                                 :cursor "pointer"}}
                [:> Icon {:style {:fontSize 18
                                  :color (if selected? "var(--color-accent)" "#8c8c8c")}}]
                [:span {:style {:textTransform "none"}}
                 label]]))]

          [:div {:style {:marginTop "auto"
                         :padding "14px 2px 0"
                         :borderTop "1px solid #ededed"}}
           [:> Button
            {:type "text"
             :block true
             :disabled (not support-link)
             :onClick (fn []
                        (when support-link
                          (.openTelegramLink web-app support-link)))
             :style {:display "flex"
                     :alignItems "center"
                     :justifyContent "flex-start"
                     :gap 10
                     :padding "10px 6px"
                     :height "auto"
                     :fontSize 16
                     :fontWeight 400
                     :color "#111"}
             :icon (as-element [:> icons/CustomerServiceOutlined {:style {:fontSize 18}}])}
            "Написать в поддержку"]
           ]
           ]
         (if @app-ready?
           [:h1 {:style {:margin 0
                         :padding "6px 0 5px"
                         :font-size 22
                         :font-weight 600
                         :line-height "100%"
                         :color "#ffffff"}}
            (get-in @settings [:telegram_bot :store_name])]
           [:div {:style {:width 140
                          :height 20
                          :borderRadius 10
                          :background "rgba(255,255,255,0.2)"}}])

         [:> Button
          {:onClick
           (fn []
             (.impactOccurred (.-HapticFeedback (.-WebApp js/Telegram)) "medium")
             (swap! app-state assoc :side_menu_open true))
           :style {:width 31

                   :border "none"
                   :background "none"
                   :transform "none"
                   :transition "none"
                   :boxShadow "none"

                   :pointer-events "auto"
                   :cursor "pointer"
                   :user-select "none"
                   :touch-action "manipulation"
                   :outline "none"
                   :webkit-tap-highlight-color "transparent"
                   :webkit-user-select "none"
                   :webkit-touch-callout "none"}

           :icon (as-element [:> icons/MenuOutlined {:style {:font-size 22
                                                             :color "#ffffff"}}])
           }
           ]
           ]
           )
           )
           )
           )
         
