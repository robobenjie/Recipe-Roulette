(function() {
  var refresh;
  refresh = function() {
    var keyword_text;
    keyword_text = $('#main-input').val();
    keyword_text = keyword_text.replace(/[^a-zA-Z\'\s]/, '');
    keyword_text = keyword_text.replace(/\s+/g, '-');
    return window.location.href = keyword_text;
  };
  $(document).ready(function() {
    $('#make-recipe-btn').on("click", refresh);
    return $('#main-input').keyup(function(e) {
      if (e.which === 13) {
        return refresh();
      }
    });
  });
}).call(this);
