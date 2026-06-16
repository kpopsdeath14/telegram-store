(ns {{name}}.pages.model-unit.modal-help
  (:require
   ["antd" :as antd]
   [{{name}}.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]])
  )

(defn modal_help []
  (let [Modal antd/Modal

        visible? (reagent/cursor app-state [:model_unit_help_modal_open?])
        help-type (reagent/cursor app-state [:model_unit_help_type])

        help-content {"common_attrs" {:title "Общие атрибуты"
                                      :description "здесь задаются значение атрибутов, общих для всех модификаций."}
                      "add_modification" {:title "Добавить модификацию"
                                          :description "создает модификацию внутри этого товара. ВАЖНО: созданная модификация изначально в архиве. чтобы она появилась в магазине нужно ее:\n1) перевести в каталог\n2) поставить ей количество в наличии > 0"}
                      "archive_toggle" {:title "Переключение архив/каталог"
                                        :description "переключение между архивом и каталогом"}
                      "archive_buttons" {:title "Кнопки архивации"
                                         :description "этими кнопками можно перевести выбранные модификации в архив (стрелка вниз) либо из архива в каталог (стрелка вверх)"}}]

    (fn []
      (let [current-help (get help-content @help-type {:title "" :description ""})]
        [:> Modal
         {:title (as-element
                  [:div {:style {:font-size "18px" :font-weight 600}}
                   (:title current-help)])
          :visible @visible?
          :closable true
          :onCancel #(swap! app-state assoc :model_unit_help_modal_open? false)
          :footer nil
          :width 600}

         [:div {:style {:white-space "pre-wrap"
                        :line-height "1.6"
                        :font-size "14px"}}
          (:description current-help)]]))))
