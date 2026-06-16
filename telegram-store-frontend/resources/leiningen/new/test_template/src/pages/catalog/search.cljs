(ns {{name}}.pages.catalog.search
  (:require
   ["antd" :as antd]
   [{{name}}.db :refer [app-state]]
   ["@ant-design/icons" :as icons]
   ["react-photo-view" :as photo_review]
   [reagent.core :as reagent :refer [as-element]]
   [clojure.string :as str]
   )
  )



(defn search []
  (let [web-app (.-WebApp js/Telegram)
        Input antd/Input
        Button antd/Button
        filters (reagent/cursor app-state [:filters_picked])
        search_value (reagent/cursor app-state [:search_value])]

    (reagent/create-class
     {:reagent-render
      (fn []
        [:div {:style {:display "flex"
                       :width "100%"
                       :gap "10px"
                       :align-items "center"}}
         
         [:> Input {:defaultValue @search_value
                    :id "catalog_search_input"
                    :size "middle"
                    :placeholder "Поиск товаров..."
                    :prefix (reagent/as-element 
                             [:> icons/SearchOutlined 
                              {:style {:color "#9a9a9a"
                                       :fontSize 16}}])
                    :style {:border "1px solid #ececec"
                            :border-radius "20px"
                            :flex "1"
                            :font-size "14px"
                            :height "40px"
                            :box-shadow "none"
                            :background "#ffffff"}
                    :onPressEnter (fn []
                                    (.hideKeyboard web-app)) 
                    :onChange (fn [event]
                                (let [value (.-value (.-target event))]
                                  (swap! app-state assoc :search_value value)))
                    :onFocus (fn []
                               (swap! app-state assoc :texting? true))
                    :onBlur (fn []
                              (swap! app-state assoc :texting? false))}]
         
         ] 
         ) 
         } 
         ) 
         ) 
         )



(comment 
  [:> Button {:type "text"
              :shape "circle"
              :size "middle"
              :onClick (fn []
                         (.impactOccurred (.-HapticFeedback (.-WebApp js/Telegram)) "light"))
              :style {:width 40
                      :height 40
                      :border "1px solid #ececec"
                      :display "flex"
                      :align-items "center"
                      :justify-content "center"
                      :padding 0}
              :icon (as-element [:img {:src "share.svg"
                                       :alt "Поделиться"
                                       :style {:width 18
                                               :height 18
                                               :display "block"}}])}]
  )