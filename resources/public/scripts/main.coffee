
refresh = ->
   keyword_text = $('#main-input').val()
   keyword_text = keyword_text.replace ///\'///, ''
   keyword_text = keyword_text.replace ///\s+///g, '-'
   window.location.href = keyword_text

$(document).ready ->
  $('#make-recipe-btn').on "click", refresh
  $('#main-input').keyup (e) ->
    refresh() if e.which is 13

