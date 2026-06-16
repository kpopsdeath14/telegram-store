(ns jemo-murge-admin-frontend.pages.product.modal-help
  (:require
   ["antd" :as antd]
   [jemo-murge-admin-frontend.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]])
  )

(defn modal_help []
  (let [Modal antd/Modal
        visible? (reagent/cursor app-state [:product_help_modal_open?])
        help-type (reagent/cursor app-state [:product_help_type])
        help-content {"archive_toggle" {:title "Перевести в архив / каталог"
                                        :description "перевод товара в архив/из архива в каталог. регулирует видно ли товар в каталоге"}
                      "fill_analogy"   {:title "Заполнить по аналогии"
                                        :description "можно заполнить атрибуты товара по аналогии с другим товаром, для этого нужен product_id (он ниже в атрибутах)"}
                      "common_attrs"   {:title "Общие атрибуты"
                                        :description "атрибуты, общие для всех модификаций товара. их можно менять только на странице товара, а не тут (страница конкретной модификации)"}
                      "images"         {:title "Изображения товара"
                                        :description "изображения конкретно этой модификации. их нужно задать для каждой модификации, порядок изображений в каталоге будет таким же как задан тут"}
                      "specific_attrs" {:title "Атрибуты конкретно этой модификации"
                                        :description "по атрибутам этой группы мы группируем товары для отображения в каталоге. комбинация их значений обязательно должна быть УНИКАЛЬНА внутри одного товара"}}]
    (fn []
      (let [current-help (get help-content @help-type {:title "" :description ""})]
        [:> Modal
         {:title (as-element [:div {:style {:font-size "18px" :font-weight 600}} (:title current-help)])
          :visible @visible?
          :closable true
          :onCancel #(swap! app-state assoc :product_help_modal_open? false)
          :footer nil
          :width 600}
         [:div {:style {:white-space "pre-wrap" :line-height "1.6" :font-size "14px"}}
          (:description current-help)]]))))
