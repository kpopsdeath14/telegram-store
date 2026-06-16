(ns jemo-murge-admin-frontend.pages.header.header
  (:require 
   ["antd" :as antd] 
   ["@ant-design/icons" :as icons] 
   [jemo-murge-admin-frontend.db :refer [app-state]] 
   [clojure.string :as string]
   [reagent.core :as reagent :refer [as-element]]
   [jemo-murge-admin-frontend.pages.modal-tutorial.modal-tutorial :refer [modal_tutorial]]
   )
  )


(defn header []
  (let [Layout antd/Layout
        Header (.-Header Layout)
        Button antd/Button
        Tour antd/Tour
        tour-open? (reagent/cursor app-state [:tour_open?])
        tour-current (reagent/cursor app-state [:tour_current])]

    (fn []
      (let [tour-steps [{:title "Нужна помощь?"
                         :description "Если у вас возникли трудности при работе с каталогом, нажмите на эту кнопку для получения инструкций"
                         :target #(js/document.querySelector ".help-button")
                         :nextButtonProps {:children "Понятно"}
                         :prevButtonProps false}]]

        (reagent/create-class
         {:component-did-mount
          (fn []
            (js/setTimeout
             (fn []
               (when (and (:first_app_launch (:development @app-state)) (not (:tour_completed @app-state)))
                 (swap! app-state assoc :tour_open? true)))
             1000))
          :reagent-render
          (fn []
            [:div
             [:> Header {:class-name "custom-header"
                         :style {:background-color "white"
                                 :display "flex"
                                 :flex-direction "row"
                                 :overflow "hidden"
                                 :flex-wrap "nowrap"
                                 :align-items "center"
                                 :justify-content "space-between"
                                 :box-shadow "0 2px 8px rgba(0, 2, 5, 0.25)"}}

              [:div {:style {:display "flex"
                             :gap 15
                             :align-items "center"}}
               [:div {:style {:font-size 24
                              :font-weight 700
                              :overflow "hidden"
                              :white-space "nowrap"
                              :flex-wrap "nowrap"}}
                "Редактировние каталога товаров"]

               [:> Button
                {:className "help-button" 
                 :shape "circle"
                 :icon (reagent/as-element [:> icons/QuestionCircleOutlined])
                 :onClick (fn []
                            (swap! app-state assoc :modal_tutorial_open? true))}]]

              [:div {:style {:font-size 36
                             :font-weight 900}}
               (let [raw-name (or (:project-name (:config @app-state)) "")
                     normalized (-> raw-name
                                    (string/replace #"(?:-admin|_admin)$" "")
                                    (string/replace #"-" "")
                                    (string/upper-case))]
                 normalized)
               ]
              ] 

             [:> Tour
              {:open @tour-open?
               :current @tour-current
               :onChange (fn [current]
                           (swap! app-state assoc :tour_current current))
               :onClose (fn []
                          (swap! app-state assoc :tour_open? false)
                          (swap! app-state assoc :tour_completed true))
               :steps tour-steps
               :type "primary"
               :placement "bottom"
               :mask true
               }
              ]

             [modal_tutorial]
             ])})))))
