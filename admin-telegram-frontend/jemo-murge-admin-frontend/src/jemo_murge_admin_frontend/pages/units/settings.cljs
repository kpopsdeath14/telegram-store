(ns jemo-murge-admin-frontend.pages.units.settings
  (:require
   ["@ant-design/icons" :as icons]
   )
  )

(defn settings []
  (fn []
    [:div {:style {:background-color "white"
                   :border "1px solid #f0f0f0"
                   :border-radius 18
                   :padding 20
                   :height "100%"
                   :cursor "pointer"
                   :transition "all 0.2s ease"
                   :box-shadow "0 2px 8px rgba(0, 0, 0, 0.08)"}
           :on-mouse-enter (fn [e]
                             (set! (.. e -currentTarget -style -boxShadow)
                                   "0 6px 16px rgba(0, 0, 0, 0.15)"))
           :on-mouse-leave (fn [e]
                             (set! (.. e -currentTarget -style -boxShadow)
                                   "0 2px 8px rgba(0, 0, 0, 0.08)"))
           :onClick (fn []
                      (set! (.-href (.-location js/window)) "#/settings"))}
     [:div {:style {:display "flex"
                    :align-items "center"
                    :justify-content "space-between"
                    :gap 12}}
      [:div {:style {:display "flex"
                     :align-items "center"
                     :gap 12}}
       [:div {:style {:width 44
                      :height 44
                      :border-radius 14
                      :display "flex"
                      :align-items "center"
                      :justify-content "center"
                      :background-color "#f5f5f5"}}
        [:> icons/SettingOutlined {:style {:font-size 20
                                           :color "#1f1f1f"}}]]
       [:div
        [:div {:style {:font-size 16
                       :font-weight 600}}
         "Настройки каталога"]
        [:div {:style {:font-size 12
                       :color "#8c8c8c"
                       :margin-top 4}}
         "Баннеры, оплаты, доставки, менеджеры"]]]
      [:> icons/RightOutlined {:style {:color "#bfbfbf"}}]]]))
