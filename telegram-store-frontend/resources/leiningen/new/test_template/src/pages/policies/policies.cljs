(ns {{name}}.pages.policies.policies
  (:require
   ["antd" :as antd]
   [{{name}}.db :refer [app-state]]
   ["@ant-design/icons" :as icons]
   ["react-photo-view" :as photo_review]
   [reagent.core :as reagent :refer [as-element]]
   [clojure.string :as str]
   )
  )


(defn policies []
  (let [
        Drawer antd/Drawer
        Button antd/Button

        policies_menu_open? (reagent/cursor app-state [:policies_menu_open?])
        ]
    (fn []
      [:> Drawer {:title "Настройки конфиденциальности"
                  :placement "bottom"
                  :open @policies_menu_open?
                  :closable false
                  :onClose (fn []
                             (swap! app-state assoc :policies_menu_open? false))
                  :height 420
                  :style {:border-radius "16px 16px 0 0"}
                  :bodyStyle {:padding "18px 20px 24px"}}
       [:div {:style {:display "flex"
                      :flexDirection "column"
                      :gap 16}} 

        [:div {:style {:border "var(--border-hairline) solid var(--color-border)"
                       :borderRadius 8
                       :padding "14px 16px"
                       :background "#ffffff"}}
         [:div {:style {:fontSize 14
                        :lineHeight "20px"
                        :color "#6f6f6f"}}
          "Продолжая использовать приложение, вы даете "
          

          [:span {:style {:color "var(--color-accent)"
                          :textDecoration "underline"
                          :cursor "pointer"}
                  :onClick (fn []
                             (set! (.-href (.-location js/window)) "#/information"))}
           "согласие на использование cookies"]
          
          
          " и аналогичных технологий для улучшения работы сервиса, а также "
          [:span {:style {:color "var(--color-accent)"
                          :textDecoration "underline"
                          :cursor "pointer"}
                  :onClick (fn []
                             (set! (.-href (.-location js/window)) "#/information"))}
           "согласие"]
          " на обработку персональных данных в соответствии с "
          [:span {:style {:color "var(--color-accent)"
                          :textDecoration "underline"
                          :cursor "pointer"}
                  :onClick (fn []
                             (set! (.-href (.-location js/window)) "#/information"))}
           "политикой конфиденциальности сервисов BIBI-ZEN"]
          "."]]

        [:> Button
         {:style {:height 56
                  :borderRadius 28
                  :width "100%"
                  :background "var(--color-accent)"
                  :border "none"
                  :fontSize 20
                  :fontWeight 400}
          :type "primary"
          :onClick (fn []
                     (swap! app-state assoc :policies_menu_open? false))}
         "Соглашаюсь"]]] 
      )
    )
  )
