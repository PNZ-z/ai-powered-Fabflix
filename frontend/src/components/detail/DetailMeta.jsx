// Label/value metadata rows shared by the movie and star detail pages.
function DetailMeta({ items }) {
  return (
    <dl className="detail-meta">
      {items.map((item) => (
        <div className="detail-meta__row" key={item.label}>
          <dt>{item.label}</dt>
          <dd>{item.value}</dd>
        </div>
      ))}
    </dl>
  )
}

export default DetailMeta
