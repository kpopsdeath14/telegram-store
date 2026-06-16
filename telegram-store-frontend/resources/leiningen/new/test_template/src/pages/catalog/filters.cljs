(ns {{name}}.pages.catalog.filters
  (:require
   ["antd" :as antd]
   [{{name}}.db :refer [app-state]]
   ["@ant-design/icons" :as icons]
   ["react-photo-view" :as photo_review]
   [reagent.core :as reagent :refer [as-element]]
   [clojure.string :as str]
  )
  )



(defn filters []
  (let [Button antd/Button
        Drawer antd/Drawer
        Radio antd/Radio
        RadioGroup (.-Group Radio)
        FilterOutlined icons/FilterOutlined

        filters_menu_open (reagent/cursor app-state [:filters_menu_open])
        sort_menu_open (reagent/cursor app-state [:sort_menu_open])]
    (fn []
      (reagent/create-class
       {:reagent-render
        (fn []
          (let [filters (reagent/cursor app-state [:filters])
                catalog_config (reagent/cursor app-state [:catalog_config])]
            [:div
             [:div {:style {:display "flex"
                            :gap "8px"
                            :align-items "center"}}
              [:> Button {:type "text"
                          :shape "circle"
                          :size "middle"
                          :onClick (fn []
                                     (reset! filters_menu_open true))
                          :style {:width 32
                                  :height 32
                                  :border "1px solid #e6e6e6"
                                  :display "flex"
                                  :align-items "center"
                                  :justify-content "center"
                                  :padding 0}
                          :icon (as-element [:> FilterOutlined {:style {:font-size 14
                                                                        :color "#6b6b6b"}}])}]
              [:> Button {:type "text"
                          :shape "circle"
                          :size "middle"
                          :onClick (fn []
                                     (swap! app-state assoc :sort_menu_open true))
                          :style {:width 32
                                  :height 32
                                  :border "1px solid #e6e6e6"
                                  :display "flex"
                                  :align-items "center"
                                  :justify-content "center"
                                  :padding 0}
                          :icon (as-element [:> icons/SortAscendingOutlined {:style {:font-size 14
                                                                                     :color "#6b6b6b"}}])}]]

             [:> Drawer {:title "Фильтры"
                         :placement "bottom"
                         :open @filters_menu_open
                         :onClose (fn []
                                    (reset! filters_menu_open false))
                         :height 560
                         :style {:border-radius "16px 16px 0 0"}
                         :bodyStyle {:padding "16px 16px 24px"}}
              [:div {:style {:display "flex"
                             :flexDirection "column"
                             :gap 18}}
               (for [{:keys [ord attribute_name form_field_type attribute_name_rus placeholder] :as field}
                     (:filters @catalog_config)]
                 (let [filter-data (first (filter (fn [f]
                                                    (= (:attribute_name f) attribute_name))
                                                  @filters))
                       options (if filter-data
                                 (vec (:attribute_values filter-data))
                                 [])
                       selected-values (set (get-in @app-state [:filters_picked (keyword attribute_name)] []))
                       ]
                   [:div {:key attribute_name}
                    [:div {:style {:display "flex"
                                   :alignItems "center"
                                   :justifyContent "space-between"
                                   :marginBottom 10}}
                     [:div {:style {:fontSize 16
                                    :fontWeight 500
                                    :color "#111"}}
                      (or attribute_name_rus attribute_name)]
                     [:button {:type "button"
                               :onClick (fn []
                                          (.impactOccurred (.-HapticFeedback (.-WebApp js/Telegram)) "light")
                                          (swap! app-state assoc-in
                                                 [:filters_picked (keyword attribute_name)]
                                                 []
                                                 )
                                          )
                               :style {:border "none"
                                       :background "transparent"
                                       :color "#8c8c8c"
                                       :fontSize 12
                                       :cursor "pointer"}}
                      "Все"]
                     ]
                    [:div {:style {:display "flex"
                                   :flexWrap "wrap"
                                   :gap 10}
                           }
                     (if (seq options)
                       (for [option options]
                         (let [selected? (contains? selected-values option)
                               ]
                           ^{:key (str attribute_name "-" option)}
                           [:button {:type "button"
                                     :className (str "filter-chip" (when selected? " filter-chip--active"))
                                     :onClick (fn []
                                                (.impactOccurred (.-HapticFeedback (.-WebApp js/Telegram)) "light")
                                                (let [current (get-in @app-state [:filters_picked (keyword attribute_name)] [])
                                                      next-values (if selected?
                                                                    (vec (remove #(= % option) current))
                                                                    (conj current option))]
                                                  (swap! app-state assoc-in
                                                         [:filters_picked (keyword attribute_name)]
                                                         next-values)))} 
                            [:span option]
                            ]
                            )
                            )
                       [:div {:style {:fontSize 12
                                      :color "#9a9a9a"}}
                        "Нет вариантов"])]]))]]

             [:> Drawer {:title "Сортировка"
                         :placement "bottom"
                         :open @sort_menu_open
                         :onClose (fn []
                                    (swap! app-state assoc :sort_menu_open false))
                         :height 400
                         :style {:border-radius "16px 16px 0 0"}
                         :bodyStyle {:padding "16px 24px"}}

              [:> RadioGroup {:value (get @app-state :selected_sorting "default")
                              :onChange (fn [e]
                                          (let [selected-value (-> e .-target .-value)]
                                            (swap! app-state assoc :selected_sorting selected-value)
                                            (swap! app-state assoc :sort_menu_open false)))
                              :style {:width "100%"}}

               (for [{:keys [sorting_key sorting_name]} (:sortings @catalog_config)]
                 ^{:key sorting_key}
                 [:> Radio {:value sorting_key
                            :style {:display "flex"
                                    :align-items "center"
                                    :width "100%"
                                    :height "52px"
                                    :margin-bottom "8px"
                                    :padding "0 16px"
                                    :border "1px solid #f0f0f0"
                                    :border-radius "8px"
                                    :transition "all 0.4s ease"}
                            :class (when (= (get @app-state :selected_sorting "default") sorting_key)
                                     "ant-radio-wrapper-checked")}
                  [:span {:style {:fontSize "16px"
                                  :fontWeight 400}}
                   sorting_name]])]]]))}))))
