(ns {{name}}.pages.modal-tutorial.modal-tutorial
  (:require
   ["antd" :as antd]
   [{{name}}.db :refer [app-state]]
   ["@ant-design/icons" :as icons]
   [reagent.core :as reagent :refer [as-element]])
  )

(defn modal_tutorial []
  (let [Modal antd/Modal
        Button antd/Button
        Card antd/Card
        Space antd/Space

        visible? (reagent/cursor app-state [:modal_tutorial_open?])
        tutorials [{:id 1 :title "Добавление товара" :desc "Создание нового товара в каталоге"}
                   {:id 2 :title "Редактирование товара" :desc "Изменение существующих товаров"}
                   {:id 3 :title "Удаление товара" :desc "Удаление товаров из системы"}
                   {:id 4 :title "Управление категориями" :desc "Создание и настройка категорий"}
                   {:id 5 :title "Загрузка изображений" :desc "Требования к изображениям товаров"}]]
    (fn []
      [:> Modal
       {:title (as-element
                [:div {:style {:font-size "18px" :font-weight 600}}
                 [:span {:style {:margin-right "8px"}} "📚"]
                 "Туториалы и инструкции"])
        :visible @visible?
        :closable true
        :onCancel #(swap! app-state assoc :modal_tutorial_open? false)
        :footer nil
        :width 700}
       
       [:> Space {:direction "vertical" :size "middle" :style {:width "100%"}}
        (for [tutorial tutorials]
          [:> Card {:key (:id tutorial)
                    :style {:margin-bottom "8px"}}
           [:div {:style {:display "flex"
                          :justify-content "space-between"
                          :align-items "center"}}
            [:div
             [:div {:style {:font-weight 600 :margin-bottom "4px"}}
              (:title tutorial)]
             [:div {:style {:color "#666" :font-size "13px"}}
              (:desc tutorial)]]
            
            [:> Button
             {:type "primary"
              :size "small"}
             "Прислать туториал"]
            ]
           ]
          )
          ]
          ]
          )
          )
          )
