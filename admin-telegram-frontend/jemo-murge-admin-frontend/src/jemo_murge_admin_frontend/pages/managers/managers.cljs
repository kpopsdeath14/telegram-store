(ns jemo-murge-admin-frontend.pages.managers.managers
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [jemo-murge-admin-frontend.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]]
   [jemo-murge-admin-frontend.api-uri-maker :refer [image_uri_maker]]
   [jemo-murge-admin-frontend.events.managers-get :refer [managers_get]]
   [jemo-murge-admin-frontend.events.managers-set :refer [managers_set]]
   [jemo-murge-admin-frontend.events.managers-request-del :refer [managers_request_del]]
   [jemo-murge-admin-frontend.pages.managers.modal-help :refer [modal_help]]
   ))


(defn managers_page []
  (let [Table antd/Table
        Avatar antd/Avatar
        Typography antd/Typography
        Button antd/Button
        Tag antd/Tag
        
        Text (.-Text Typography)
        
        managers (reagent/cursor app-state [:managers])
        owners (reagent/cursor app-state [:owners])
        managers_request (reagent/cursor app-state [:managers_request])
        development (reagent/cursor app-state [:development])

        columns [{:title "ID"
                  :dataIndex "id"
                  :key "id"
                  :width 150
                  :render (fn [_ record]
                            (let [manager (js->clj record :keywordize-keys true)
                                  id (:id manager)
                                  tg-link (if-let [username (:username manager)]
                                            (str "https://t.me/" username)
                                            (str "tg://user?id=" (:id manager)))]
                              (as-element
                               [:> Button
                                {:size "small"
                                 :href tg-link
                                 :target "_blank"
                                 :icon (reagent/as-element
                                        [:i {:class "fas fa-paper-plane"}])}
                                (str id)])))}
  
                 {:title "Аватар"
                  :key "avatar"
                  :width 70
                  :render (fn [_ record]
                            (let [
                                  manager (js->clj record :keywordize-keys true)
                                  ] 
                              (as-element
                               [:> Avatar
                                {:src (when-let [photo (:photo manager)]
                                        (or (:big_file_url photo)
                                            (:big_file_id photo)))
                                 :size "large"
                                 :style {:background-color "#1890ff"}}
                                (when-not (:photo manager)
                                  (-> manager :first_name first)
                                  )
                                ]
                               )
                              )
                            )
                  }
  
                 {:title "Имя"
                  :key "name"
                  :render (fn [_ record]
                            (let [manager (js->clj record :keywordize-keys true)]
                              (as-element
                               [:div
                                [:div {:style {:font-weight 500
                                               :font-size "14px"}}
                                 (str (:first_name manager) " " (:last_name manager))]
                               
                                (when-let [username (:username manager)]
                                  [:> Tag {:color "blue"
                                           :style {:margin-top "4px"
                                                   :font-size "12px"}}
                                   (str "@" username)]
                                  )
                                ]
                               )
                              )
                            )
                  }
  
                 {:title "Действия"
                  :key "actions"
                  :width 120
                  :render (fn [_ record] 
                            (let [
                                  manager (js->clj record :keywordize-keys true)
                                  tg-link (if-let [username (:username manager)]
                                            (str "https://t.me/" username)
                                            (str "tg://user?id=" (:id manager)))
                                  ]
                              (as-element
                               [:> Button
                                {:type "primary" 
                                 :onClick (fn []
                                            (managers_set [{:telegram_user_id (:id manager)
                                                            :user_status "normis"}]))
                                 :style {:background-color "#ff4d4f"
                                         :border-color "#ff4d4f"
                                         :width "100%"
                                         }
                                 }
                                "Удалить"
                                ]
                               )
                              )
                            )
                 }
                 ]

                 columns_owners [{:title "ID"
                                  :dataIndex "id"
                                  :key "id"
                                  :width 150
                                  :render (fn [_ record]
                                            (let [owner (js->clj record :keywordize-keys true)
                                                  id (:id owner)
                                                  tg-link (if-let [username (:username owner)]
                                                            (str "https://t.me/" username)
                                                            (str "tg://user?id=" (:id owner)))]
                                              (as-element
                                               [:> Button
                                                {:size "small"
                                                 :href tg-link
                                                 :target "_blank"
                                                 :icon (reagent/as-element
                                                        [:i {:class "fas fa-paper-plane"}])}
                                                (str id)])))}
                 
                                 {:title "Аватар"
                                  :key "avatar"
                                  :width 70
                                  :render (fn [_ record]
                                            (let [owner (js->clj record :keywordize-keys true)]
                                              (as-element
                                               [:> Avatar
                                                  {:src (when-let [photo (:photo owner)]
                                                          (or (:big_file_url photo)
                                                              (:big_file_id photo)))
                                                   :size "large"
                                                   :style {:background-color "#1890ff"}}
                                                (when-not (:photo owner)
                                                  (-> owner :first_name first))])))}
                 
                                 {:title "Имя"
                                  :key "name"
                                  :render (fn [_ record]
                                            (let [owner (js->clj record :keywordize-keys true)]
                                              (as-element
                                               [:div
                                                [:div {:style {:font-weight 500
                                                               :font-size "14px"}}
                                                 (str (:first_name owner) " " (:last_name owner))]
                 
                                                (when-let [username (:username owner)]
                                                  [:> Tag {:color "blue"
                                                           :style {:margin-top "4px"
                                                                   :font-size "12px"}}
                                                   (str "@" username)])
                                                [:> Tag {:color "gold"
                                                         :style {:margin-top "6px"
                                                                 :font-size "11px"}}
                                                 "Владелец"]]
                                               )
                                              )
                                            )
                                  }
                                 ]


                 columns_request  [{:title "ID"
                                    :dataIndex "id"
                                    :key "id"
                                    :width 150
                                    :render (fn [_ record]
                                              (let [manager (js->clj record :keywordize-keys true)
                                                    id (:id manager)
                                                    tg-link (if-let [username (:username manager)]
                                                              (str "https://t.me/" username)
                                                              (str "tg://user?id=" (:id manager)))
                                                    ]
                                                (as-element
                                                 [:> Button
                                                  {:size "small"
                                                   :href tg-link
                                                   :target "_blank"
                                                   :icon (reagent/as-element
                                                          [:i {:class "fas fa-paper-plane"}])}
                                                  (str id)])))}
                                  
                                   {:title "Аватар"
                                    :key "avatar"
                                    :width 70
                                    :render (fn [_ record]
                                              (let [manager (js->clj record :keywordize-keys true)]
                                                (as-element
                                                 [:> Avatar
                                                  {:src (when-let [photo (:photo manager)]
                                                          (or (:big_file_url photo)
                                                              (:big_file_id photo)))
                                                   :size "large"
                                                   :style {:background-color "#1890ff"}}
                                                  (when-not (:photo manager)
                                                    (-> manager :first_name first))])))}
                                  
                                   {:title "Имя"
                                    :key "name"
                                    :render (fn [_ record]
                                              (let [manager (js->clj record :keywordize-keys true)]
                                                (as-element
                                                 [:div
                                                  [:div {:style {:font-weight 500
                                                                 :font-size "14px"}}
                                                   (str (:first_name manager) " " (:last_name manager))]
                                  
                                                  (when-let [username (:username manager)]
                                                    [:> Tag {:color "blue"
                                                             :style {:margin-top "4px"
                                                                     :font-size "12px"}}
                                                     (str "@" username)])])))}
                                  
                                   {:title "Действия"
                                    :key "actions"
                                    :width 120
                                    :render (fn [_ record]
                                              (let [
                                                    manager (js->clj record :keywordize-keys true)
                                                    ]
                                                (as-element
                                                 [:div {:style {:display "flex"
                                                                :gap "8px"
                                                                :flex-direction "column"
                                                                }
                                                        }
                                                  [:> Button
                                                   {:type "primary"
                                                    :onClick (fn []
                                                               (managers_set [{:telegram_user_id (:id manager) 
                                                                               :user_status "manager"
                                                                               }
                                                                              ]
                                                                             )
                                                               )
                                                    :style {:background-color "#52c41a"
                                                            :border-color "#52c41a"}}
                                                   "Принять"]
                                  
                                                  [:> Button
                                                   {:type "primary"
                                                    :danger true
                                                    :style {:background-color "#ff4d4f"
                                                            :border-color "#ff4d4f"}
                                                    :onClick (fn []
                                                               (managers_request_del (:id manager))
                                                               )
                                                    }
                                                   "Отклонить"]]
                                                   )
                                                   )
                                                   )
                                                   }
                                                   ]
                 ]
    (fn []
      (if (= "manager" (:user_status @development))
        [:div {:style {:display "flex"
                       :flex-direction "column"
                       :justify-content "center"
                       :align-items "center"
                       :height "100vh"
                       :width "100vw"
                       :background-color "#fff"}}
         [:div {:style {:position "relative"
                        :width "100px"
                        :height "100px"
                        :margin-bottom "30px"}}
          [:div {:style {:position "absolute"
                         :top 0
                         :left 0
                         :width "100%"
                         :height "100%"
                         :border "8px solid #f0f0f0"
                         :border-top-color "#1890ff"
                         :border-radius "50%"
                         :animation "spin 1s linear infinite"}}]
          [:div {:style {:position "absolute"
                         :top "50%"
                         :left "50%"
                         :transform "translate(-50%, -50%)"
                         :font-size "2rem"
                         :color "#ff4d4f"}}
           "⛔"]]
         [:h1 {:style {:color "#262626"
                       :font-size "2rem"
                       :font-weight 600
                       :margin-bottom "15px"}}
          "Доступ ограничен"]
         [:p {:style {:color "#8c8c8c"
                      :margin-bottom "25px"
                      :max-width "300px"
                      :text-align "center"}}
          "Эта страница доступна только администраторам и владельцам."]
         [:> antd/Button
          {:type "link"
           :onClick (fn []
                      (set! (.-href (.-location js/window)) "#/settings")
                      )
           }
          "Вернуться назад"]
         [:style
          "@keyframes spin {
             0% { transform: rotate(0deg); }
             100% { transform: rotate(360deg); }
           }"]
         ]
        [:div
         [modal_help]
         [:div {:style {:display "flex"
                        :align-items "center"
                        :gap 16
                        :margin-bottom 24}}
          [:> Button {:style {:background "#D3EAFF"
                              :color "black"
                              :height 44
                              :border-radius 14
                              :font-size 16
                              :font-weight 400
                              :box-shadow "0 2px 8px rgba(0, 2, 5, 0.15)"}
                      :icon (as-element [:> icons/ArrowLeftOutlined])
                      :onClick (fn []
                                 (set! (.-href (.-location js/window)) "#/settings"))}
           "Назад"]
          [:div
           [:div {:style {:display "flex" :align-items "center" :gap 8}}
            [:div {:style {:font-size 24
                           :font-weight 700}}
             "Менеджеры"]
            [:> Button {:shape "circle"
                        :icon (as-element [:> icons/QuestionCircleOutlined])
                        :onClick (fn []
                                   (swap! app-state assoc :managers_help_type "managers_page")
                                   (swap! app-state assoc :managers_help_modal_open? true))}]]
           [:div {:style {:font-size 13
                          :color "#8c8c8c"
                          :margin-top 4}}
            "Управление доступом и заявками"]]]

         [:div {:style {:display "flex" :align-items "center" :gap 8}}
          [:div {:style {:font-size 24
                         :font-weight 700}}
           "Владельцы"]
          [:> Button {:shape "circle"
                      :icon (as-element [:> icons/QuestionCircleOutlined])
                      :onClick (fn []
                                 (swap! app-state assoc :managers_help_type "owners")
                                 (swap! app-state assoc :managers_help_modal_open? true))}]]
         [:> Table
          {:columns columns_owners
           :dataSource @owners
           :rowKey "id"
           :pagination false
           :size "middle"
           :style {:margin-top "20px"
                   :margin-bottom "30px"}}]

         [:div {:style {:display "flex" :align-items "center" :gap 8}}
          [:div {:style {:font-size 24
                         :font-weight 700}}
           "Менеджеры"]
          [:> Button {:shape "circle"
                      :icon (as-element [:> icons/QuestionCircleOutlined])
                      :onClick (fn []
                                 (swap! app-state assoc :managers_help_type "managers_list")
                                 (swap! app-state assoc :managers_help_modal_open? true))}]]
         [:> Table
          {:columns columns
           :dataSource @managers
           :rowKey "id"
           :pagination false
           :size "middle"
           :style {:margin-top "20px"}}]

         [:div {:style {:display "flex" :align-items "center" :gap 8}}
          [:div {:style {:font-size 24
                         :font-weight 700}}
           "Заявки на менеджера"]
          [:> Button {:shape "circle"
                      :icon (as-element [:> icons/QuestionCircleOutlined])
                      :onClick (fn []
                                 (swap! app-state assoc :managers_help_type "requests")
                                 (swap! app-state assoc :managers_help_modal_open? true))}]]
         

         [:> Table
          {:columns columns_request
           :dataSource @managers_request
           :rowKey "id"
           :pagination false
           :size "middle"
           :style {:margin-top "20px"}}]
         ]
        ) 
      )
    )
  )
